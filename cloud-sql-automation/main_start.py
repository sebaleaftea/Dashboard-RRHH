import googleapiclient.discovery

def start_instance(request):
    project = "prd-sfh-it-bi-erbi"
    instance = "gdh-massti"
    service = googleapiclient.discovery.build('sqladmin', 'v1beta4')
    request = service.instances().patch(
        project=project,
        instance=instance,
        body={"settings": {"activationPolicy": "ALWAYS"}}
    )
    response = request.execute()
    return f"Started instance: {instance}"
