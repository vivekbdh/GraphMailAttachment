import com.microsoft.graph.concurrency.ChunkedUploadProvider;
import com.microsoft.graph.concurrency.IProgressCallback;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.models.extensions.*;
import com.microsoft.graph.models.generated.AttachmentType;
import com.microsoft.graph.requests.extensions.GraphServiceClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;

public class GraphMailApplication {

    public static IProgressCallback<AttachmentItem> callback = new IProgressCallback<AttachmentItem> () {
        @Override
        public void progress(final long current, final long max) {
            //Check progress
        }
        @Override
        public void success(final AttachmentItem result) {
            //Handle the successful response
        }

        @Override
        public void failure(final ClientException ex) {
            ex.printStackTrace();
        }
    };

    public static void main(String[] a){
        try {
            AuthenticationProvider mailAuthenticationProvider = new AuthenticationProvider();

            IGraphServiceClient graphClient = GraphServiceClient
                    .builder()
                    .authenticationProvider(mailAuthenticationProvider)
                    .logger(new MicrosoftGraphLogger())
                    .buildClient();

            String draftSubject = "Draft Test Message " + Double.toString(Math.random()*1000);
            User me = graphClient.me().buildRequest().get();
            Recipient r = new Recipient();
            EmailAddress address = new EmailAddress();
            address.address = "vivekbdh@gmail.com";
            r.emailAddress = address;
            Message message = new Message();
            message.subject = draftSubject;
            ArrayList<Recipient> recipients = new ArrayList<Recipient>();
            recipients.add(r);
            message.toRecipients = recipients;
            message.isDraft = true;

            //Save the message as a draft
            Message newMessage = graphClient.me().messages().buildRequest().post(message);

            File file = new File("/home/user/Downloads/slides.pdf");

            AttachmentItem attachmentItem = new AttachmentItem();
            attachmentItem.attachmentType = AttachmentType.FILE;
            attachmentItem.name = file.getName();
            attachmentItem.size = file.length();
            attachmentItem.contentType = "application/octet-stream";

            InputStream fileStream = new FileInputStream("/home/user/Downloads/slides.pdf");

            long streamSize = attachmentItem.size;

            UploadSession uploadSession = graphClient.me()
                    .messages(newMessage.id)
                    .attachments()
                    .createUploadSession(attachmentItem)
                    .buildRequest()
                    .post();

            ChunkedUploadProvider<AttachmentItem> chunkedUploadProvider = new ChunkedUploadProvider<>(uploadSession, graphClient, fileStream,
                    streamSize, AttachmentItem.class);

            // Do the upload
            chunkedUploadProvider.upload(callback);

            //Send the drafted message
            graphClient.me().mailFolders("Drafts").messages(newMessage.id).send().buildRequest().post();
            System.out.println("Mail Sent Successfully");
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
