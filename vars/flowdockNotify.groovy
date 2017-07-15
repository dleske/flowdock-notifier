// this was cribbed from https://github.com/jenkinsci/flowdock-plugin/issues/24
import groovy.json.JsonOutput
import java.net.URLEncoder
import hudson.model.Result

def call(script, apiToken, tags = '') {

    tags = tags.replaceAll("\\s","")

    def flowdockURL = "https://api.flowdock.com/v1/messages/team_inbox/${apiToken}"
    def fromAddress = 'noreply+jenkins@computecanada.ca'

    // build status of null means successful
    def buildStatus =  script.currentBuild.result ? script.currentBuild.result : 'SUCCESS'

    // create subject and update with build status
    def subject = "${script.env.JOB_BASE_NAME} build ${script.currentBuild.displayName.replaceAll("#", "")}"

    switch (buildStatus) {
      case 'SUCCESS':
        def prevResult = script.currentBuild.getPreviousBuild() != null ? script.currentBuild.getPreviousBuild().getResult() : null;
        if (Result.FAILURE.toString().equals(prevResult) || Result.UNSTABLE.toString().equals(prevResult)) {
          subject += ' was fixed'
          break
        }
        subject += ' was successful'
        break
      case 'FAILURE':
        subject += ' failed'
        break
      case 'UNSTABLE':
        subject += ' was unstable'
        break
      case 'ABORTED':
        subject += ' was aborted'
        break
      case 'NOT_BUILT':
        subject += ' was not built'
        break
      case 'FIXED':
        subject = ' was fixed'
        break
    }

    // build message
    def content = """<h3>${script.env.JOB_BASE_NAME}</h3>
      Build: ${script.currentBuild.displayName}<br />
      Result: <strong>${buildStatus}<br />
      URL: <a href="${script.env.BUILD_URL}">${script.currentBuild.fullDisplayName}</a><br />"""

    // build payload
    def payload = JsonOutput.toJson([source : "Jenkins",
                                     project : script.env.JOB_BASE_NAME,
                                     from_address: fromAddress,
                                     from_name: 'Jenkins',
                                     subject: subject,
                                     tags: tags,
                                     content: content,
                                     link: script.env.BUILD_URL
                                     ])

    // craft and send the request
    def post = new URL(flowdockURL).openConnection();
    post.setRequestMethod("POST");
    post.setDoOutput(true);
    post.setRequestProperty("Content-Type", "application/json")
    post.getOutputStream().write(payload.getBytes("UTF-8"));
    def postRC = post.getResponseCode();

    println("Response received from Flowdock API: " + postRC);
}
