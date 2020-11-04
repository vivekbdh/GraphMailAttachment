import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.oauth2.clientauthentication.RequestBodyAuthenticationScheme;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;

public class MicrosoftAzureAD20Api extends DefaultApi20 {

    private static String microsoftUrl = "https://login.microsoftonline.com";
    private static String tenantId = "";

    MicrosoftAzureAD20Api(String tenantId){
        this.tenantId = tenantId;
    }

    @Override
    public String getAccessTokenEndpoint() {
        return microsoftUrl+"/"+tenantId+"/oauth2/v2.0/token";
    }

    @Override
    protected String getAuthorizationBaseUrl() {
        return microsoftUrl+"/"+tenantId+"/oauth2/v2.0/authorize";
    }

    @Override
    public RequestBodyAuthenticationScheme getClientAuthentication(){
        return RequestBodyAuthenticationScheme.instance();
    }
}
