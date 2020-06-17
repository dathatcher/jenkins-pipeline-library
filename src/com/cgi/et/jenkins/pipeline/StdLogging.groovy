package com.cgi.et.jenkins.pipeline

class StdLogging {

  Map map
  Object logger
  String title

  StdLogging() {
    this.map = map
    this.logger = logger
    this.title = title
  }

  static void logParameters(Object scope, Map map, String title) {
    String message = "##### BEGIN: Log Parameters: ${title}\r\n"
    message += logMap(map, "")
    message += "##### END: Log Parameters: ${title}"
    scope.println(message)
  }

  private static String logMap(Map map, String spacing) {
    //TODO: replace with StringBuilder?
    String message = ""
    for (Map.Entry entry : map.entrySet()) {
      String key = entry.getKey()
      Object value = entry.getValue()
      if (value instanceof Map) {
        message += spacing + key + ":\r\n" + logMap((Map) value, spacing + "  ")
      } else {
        message += spacing + key + ": " + value + "\r\n"
      }
    }
    return message
  }

}
