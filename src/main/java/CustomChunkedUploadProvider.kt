import com.microsoft.graph.concurrency.ChunkedUploadResponseHandler
import com.microsoft.graph.concurrency.IProgressCallback
import com.microsoft.graph.models.extensions.IGraphServiceClient
import com.microsoft.graph.models.extensions.UploadSession
import com.microsoft.graph.options.Option
import com.microsoft.graph.requests.extensions.ChunkedUploadRequest
import java.io.IOException
import java.io.InputStream
import java.security.InvalidParameterException

/**
 * Based on the com.microsoft.graph.concurrency.ChunkedUploadProvider but this implementation tries to read the inputstream until the chunk buffer is full or the stream is closed.
 * This is done to make sure the chuck buffer is as full as possible for optimal performance. Without this change the situation can occur that an 100mb file
 * is uploaded in 4kb chunks, which takes forever.
 */
class CustomChunkedUploadProvider<UploadType>(
        uploadSession: UploadSession?,
        client: IGraphServiceClient?,
        inputStream: InputStream?,
        streamSize: Int,
        uploadTypeClass: Class<UploadType>?
) {

    companion object {
        /**
         * The default chunk size for upload. Currently set to 5 MiB.
         */
        private const val DEFAULT_CHUNK_SIZE = 5 * 1024 * 1024

        /**
         * The maximum chunk size for a single upload allowed by OneDrive service.
         * Currently the value is 60 MiB.
         */
        private const val MAXIMUM_CHUNK_SIZE = 60 * 1024 * 1024

        /**
         * The default retry times for a simple chunk upload if failure happened
         */
        private const val MAXIMUM_RETRY_TIMES = 3

    }

    /**
     * The client
     */
    private val client: IGraphServiceClient

    /**
     * The input stream
     */
    private val inputStream: InputStream

    /**
     * The upload session URL
     */
    private val uploadUrl: String

    /**
     * The stream size
     */
    private val streamSize: Int

    /**
     * The upload response handler
     */
    private val responseHandler: ChunkedUploadResponseHandler<UploadType>

    /**
     * The counter for how many bytes have been read from input stream
     */
    private var readSoFar: Int


    /**
     * Creates the ChunkedUploadProvider
     *
     * @param uploadSession   the initial upload session
     * @param client          the Graph client
     * @param inputStream     the input stream
     * @param streamSize      the stream size
     * @param uploadTypeClass the upload type class
     */
    init {
        if (uploadSession == null) {
            throw InvalidParameterException("Upload session is null.")
        }
        if (client == null) {
            throw InvalidParameterException("OneDrive client is null.")
        }
        if (inputStream == null) {
            throw InvalidParameterException("Input stream is null.")
        }
        if (streamSize <= 0) {
            throw InvalidParameterException("Stream size should larger than 0.")
        }
        this.client = client
        readSoFar = 0
        this.inputStream = inputStream
        this.streamSize = streamSize
        uploadUrl = uploadSession.uploadUrl
        responseHandler = ChunkedUploadResponseHandler(uploadTypeClass)
    }

    /**
     * Uploads content to remote upload session based on the input stream
     *
     * @param options  the upload options
     * @param callback the progress callback invoked during uploading
     * @param configs  the optional configurations for the upload options. [0] should be the customized chunk
     * size and [1] should be the maxRetry for upload retry.
     * @throws IOException the IO exception that occurred during upload
     */
    @Throws(IOException::class)
    fun upload(
            options: List<Option?>?,
            callback: IProgressCallback<UploadType>,
            vararg configs: Int
    ) {
        var chunkSize: Int = DEFAULT_CHUNK_SIZE
        if (configs.size > 0) {
            chunkSize = configs[0]
        }
        var maxRetry: Int = MAXIMUM_RETRY_TIMES
        if (configs.size > 1) {
            maxRetry = configs[1]
        }
        require(chunkSize <= MAXIMUM_CHUNK_SIZE) { "Please set chunk size smaller than 60 MiB" }
        val buffer = ByteArray(chunkSize)
        loop@ while (readSoFar < streamSize) {

            var tmpRead = 0
            do {
                val read = inputStream.read(buffer, tmpRead, buffer.size - tmpRead)
                if (read == -1 && tmpRead == 0) {
                    break@loop
                } else if (read > 0) {
                    tmpRead += read
                }
            } while (tmpRead < buffer.size && read > 0)

            val request = ChunkedUploadRequest(uploadUrl, client, options, buffer, tmpRead, maxRetry, readSoFar.toLong(), streamSize.toLong())
            val result = request.upload(responseHandler)
            if (result.uploadCompleted()) {
                callback.progress(streamSize.toLong(), streamSize.toLong())
                callback.success(result.item as UploadType)
                break
            } else if (result.chunkCompleted()) {
                callback.progress(readSoFar.toLong(), streamSize.toLong())
            } else if (result.hasError()) {
                callback.failure(result.error)
                break
            }
            readSoFar += tmpRead
        }
    }

    /**
     * Uploads content to remote upload session based on the input stream
     *
     * @param callback the progress callback invoked during uploading
     * @param configs  the optional configurations for the upload options. [0] should be the customized chunk
     * size and [1] should be the maxRetry for upload retry.
     * @throws IOException the IO exception that occurred during upload
     */
    @Throws(IOException::class)
    fun upload(
            callback: IProgressCallback<UploadType>,
            vararg configs: Int
    ) {
        upload(null, callback, *configs)
    }

}
