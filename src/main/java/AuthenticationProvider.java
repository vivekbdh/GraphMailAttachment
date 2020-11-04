import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.github.scribejava.core.oauth.OAuthService;
import com.microsoft.graph.authentication.IAuthenticationProvider;
import com.microsoft.graph.http.IHttpRequest;

class AuthenticationProvider implements IAuthenticationProvider {

    private String clientId = "CLIENT-ID";
    private String clientSecret = "CLIENT-SECRET";
    private String tenantId = "TENENT-ID";
    private String refeshToken = "REFRESH-TOKEN";

    @Override
    public void authenticateRequest(IHttpRequest request) {
        OAuth20Service oAuth20Service = new ServiceBuilder(clientId)
                .callback("https://google.com")
                .defaultScope("User.Read Mail.Send offline_access")
                .apiKey(clientId)
                .apiSecret(clientSecret).build(new MicrosoftAzureAD20Api(tenantId));

        try {
            OAuth2AccessToken oAuth2AccessToken = oAuth20Service.refreshAccessToken(refeshToken);
            System.out.println("Access Toke:::"+oAuth2AccessToken.getAccessToken());
            if(request.getRequestUrl().getHost().toLowerCase().contains("graph")){
                request.getHeaders().removeIf(it -> it.getName() == "Authorization");
                request.addHeader("Authorization", "Bearer "+oAuth2AccessToken.getAccessToken());
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
