/********************** CONFIGURATION ************************/
recipients = "cloudwatchgroup"
ATTACH_GRAPHS = true
def snsMessageConfig = [
    ADDITIONAL_METRICS:[CPUUtilization:["DiskReadBytes", "NetworkIn", "NetworkOut" , "DiskWriteBytes"]],
    AWS_ACCESS_KEY:"<amazon_access_key>",
    AWS_SECRET_KEY:"<amazon_secret_key>",
    LAST_N_DAYS:10,
    STAT_PERIOD:300,
]
/*************************************************************/
AmazonSnsMessage snsMessage = new AmazonSnsMessage(request, snsMessageConfig)
/*********************************************/
if(AmazonSnsMessage.MESSAGE_TYPE_SUBSCRIPTION_CONFIRMATION.equals(snsMessage.getRequestType())){
    snsMessage.confirmSubscription();
}
else if(AmazonSnsMessage.MESSAGE_TYPE_NOTIFICATION.equals(snsMessage.getRequestType())){
    def alertId = createAlert(snsMessage);
    if(ATTACH_GRAPHS){
        attachMetricGraphs(snsMessage, alertId);
    }
}

def createAlert(AmazonSnsMessage snsMessage){
    String alertDescription = snsMessage.getCloudwatchAlertDescription()
    Map details = snsMessage.getCloudwatchAlertDetails()
    String subject = snsMessage.getSubject()
    def alertProps = [recipients:recipients, message:subject,  details:details, description:alertDescription]
    logger.warn("Creating alert with message ${subject}");
    def response = opsgenie.createAlert(alertProps)
    def alertId =  response.alertId;
    logger.warn("Alert is created with id :"+alertId);
    return alertId;
}

def attachMetricGraphs(snsMessage, alertId){
    def graphs = snsMessage.createMetricGraphs()
    graphs.each{graph->
        logger.warn("Attaching graphs ${graph.name}");
        def response = opsgenie.attach([alertId:alertId, stream:new ByteArrayInputStream(graph.data), fileName:graph.name])
        if(response.success){
            logger.warn("Successfully attached search results as ${graph.name}");
        }
    }
}