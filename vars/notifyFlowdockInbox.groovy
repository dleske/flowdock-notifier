// this was cribbed from https://github.com/jenkinsci/flowdock-plugin/issues/24
import groovy.json.JsonOutput
import java.net.URLEncoder
import hudson.model.Result

def notifyFlowdockInbox(apiToken, tags) {
    tags = tags.replaceAll("\\s","")
    // build status of null means successful
    def buildStatus =  currentBuild.result ? currentBuild.result : 'SUCCESS'
    def subject = "${env.JOB_BASE_NAME} build ${currentBuild.displayName.replaceAll("#", "")}"
    def fromAddress = ''
    switch (buildStatus) {
      case 'SUCCESS':
        def prevResult = currentBuild.getPreviousBuild() != null ? currentBuild.getPreviousBuild().getResult() : null;
        if (Result.FAILURE.toString().equals(prevResult) || Result.UNSTABLE.toString().equals(prevResult)) {
          subject += ' was fixed'
          fromAddress = 'build+ok@flowdock.com'
          break
        }
        subject += ' was successful'
        fromAddress = 'build+ok@flowdock.com'
        break
      case 'FAILURE':
        subject += ' failed'
        fromAddress = 'build+fail@flowdock.com'
        break
      case 'UNSTABLE':
        subject += ' was unstable'
        fromAddress = 'build+fail@flowdock.com'
        break
      case 'ABORTED':
        subject += ' was aborted'
        fromAddress = 'build+fail@flowdock.com'
        break
      case 'NOT_BUILT':
        subject += ' was not built'
        fromAddress = 'build+fail@flowdock.com'
      case 'FIXED':
        subject = ' was fixed'
        fromAddress = 'build+ok@flowdock.com'
        break
    }
    StringBuilder content = new StringBuilder();
    content.append("<h3>").append(env.JOB_BASE_NAME).append("</h3>");
    content.append("Build: ").append(currentBuild.displayName).append("<br />");
    content.append("Result: <strong>").append(buildStatus).append("</strong><br />");
    content.append("URL: <a href=\"").append(env.BUILD_URL).append("\">").append(currentBuild.fullDisplayName).append("</a>").append("<br />");
    def flowdockURL = "https://api.flowdock.com/v1/messages/team_inbox/${apiToken}"
    def payload = JsonOutput.toJson([source : "Jenkins",
                                     project : env.JOB_BASE_NAME,
                                     from_address: fromAddress,
                                     from_name: 'CI',
                                     subject: subject,
                                     tags: tags,
                                     content: content,
                                     link: env.BUILD_URL
                       ])
    def post = new URL(flowdockURL).openConnection();
    post.setRequestMethod("POST");
    post.setDoOutput(true);
    post.setRequestProperty("Content-Type", "application/json")
    post.getOutputStream().write(payload.getBytes("UTF-8"));
    def postRC = post.getResponseCode();
    if (postRC.equals(200)) {
      // whoop
    {
}

