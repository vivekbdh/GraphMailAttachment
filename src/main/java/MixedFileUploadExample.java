import com.microsoft.graph.concurrency.IProgressCallback;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.models.extensions.AttachmentItem;
import com.microsoft.graph.models.extensions.User;
import com.microsoft.graph.models.extensions.Recipient;
import com.microsoft.graph.models.extensions.EmailAddress;
import com.microsoft.graph.models.extensions.Message;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.models.extensions.UploadSession;
import com.microsoft.graph.models.generated.AttachmentType;
import com.microsoft.graph.requests.extensions.GraphServiceClient;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MixedFileUploadExample {

    public static IProgressCallback<AttachmentItem> callback = new IProgressCallback<AttachmentItem> () {
        @Override
        public void progress(final long current, final long max) {}

        @Override
        public void success(final AttachmentItem result) {}

        @Override
        public void failure(final ClientException ex) {
            ex.printStackTrace();
        }
    };

    public static void main(String[] a){
        try {
            AuthenticationProvider authenticationProvider = new AuthenticationProvider();

            IGraphServiceClient graphClient = GraphServiceClient
                    .builder()
                    .authenticationProvider(authenticationProvider)
                    .logger(new MicrosoftGraphLogger())
                    .buildClient();

            Recipient r = new Recipient();
            EmailAddress address = new EmailAddress();
            address.address = "vivektest@gamil.com";
            r.emailAddress = address;
            Message message = new Message();
            message.subject = "Test mixed file size Attachments";
            ArrayList<Recipient> recipients = new ArrayList<Recipient>();
            recipients.add(r);
            message.toRecipients = recipients;
            message.isDraft = true;

            List<File> fileList = new ArrayList();
            fileList.add(new File("/home/user/Downloads/210KBPDF.pdf"));
            fileList.add(new File("/home/user/Downloads/1MBPDF.pdf"));
            fileList.add(new File("/home/user/Downloads/4MBPDF.pdf"));
            //Save the message as a draft
            Message newMessage = graphClient.me().messages().buildRequest().post(message);

            for(File file:fileList){
                InputStream fileStream = new FileInputStream(file);

                AttachmentItem attachmentItem = new AttachmentItem();
                attachmentItem.attachmentType = AttachmentType.FILE;
                attachmentItem.name = file.getName();
                attachmentItem.size = file.length();
                attachmentItem.contentType = "application/pdf";

                long streamSize = attachmentItem.size;

                UploadSession uploadSession = graphClient.me()
                        .messages(newMessage.id)
                        .attachments()
                        .createUploadSession(attachmentItem)
                        .buildRequest()
                        .post();

                CustomChunkedUploadProvider<AttachmentItem> chunkedUploadProvider = new CustomChunkedUploadProvider(uploadSession, graphClient, fileStream,
                        (int) streamSize, AttachmentItem.class);
                // Do the upload
                chunkedUploadProvider.upload(callback);
            }
            //Send the drafted message
            graphClient.me().mailFolders("Drafts").messages(newMessage.id).send().buildRequest().post();
            System.out.println("Mail Sent Successfully");
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
