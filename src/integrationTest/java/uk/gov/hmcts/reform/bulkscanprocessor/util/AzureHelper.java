package uk.gov.hmcts.reform.bulkscanprocessor.util;

public final class AzureHelper {

    private AzureHelper() {
    }

    public static final String AZURE_TEST_CONTAINER = "hmctspublic.azurecr.io/imported/azure-storage/azurite:3.29.0";
    public static final String EXTRACTION_HOST = "azurite";
    public static final int CONTAINER_PORT = 10000;
    public static final String CONTAINER_NAME = "bulkscan";


}
