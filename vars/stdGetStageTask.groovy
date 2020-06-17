import com.cgi.et.jenkins.pipeline.StdUtils

def call(Map stagesMap, String stageName) {

  StdUtils task = new StdUtils(this)
  task.getCurrentTask(stagesMap, stageName)

}
