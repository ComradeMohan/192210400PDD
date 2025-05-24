package com.simats.univalut

import com.android.volley.*
import com.android.volley.toolbox.HttpHeaderParser
import java.io.*
import java.util.*

class VolleyFileUpload(
    method: Int,
    url: String,
    private val listener: Response.Listener<NetworkResponse>,
    errorListener: Response.ErrorListener
) : Request<NetworkResponse>(method, url, errorListener) {

    private val boundary = "volley_boundary_${System.currentTimeMillis()}"
    private val mimeType = "multipart/form-data; boundary=$boundary"

    private var params: Map<String, String> = mapOf()
    private var fileData: ByteArray? = null
    private var fileName: String = "file"

    fun setParams(params: Map<String, String>) {
        this.params = params
    }

    fun setFile(fileData: ByteArray, fileName: String) {
        this.fileData = fileData
        this.fileName = fileName
    }

    override fun getBodyContentType(): String = mimeType

    override fun getBody(): ByteArray {
        val bos = ByteArrayOutputStream()
        val writer = PrintWriter(OutputStreamWriter(bos, "UTF-8"), true)

        // Add text parameters
        for ((key, value) in params) {
            writer.append("--$boundary\r\n")
            writer.append("Content-Disposition: form-data; name=\"$key\"\r\n\r\n")
            writer.append(value).append("\r\n")
        }

        // Add file
        fileData?.let {
            writer.append("--$boundary\r\n")
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"\r\n")
            writer.append("Content-Type: application/octet-stream\r\n\r\n")
            writer.flush()
            bos.write(it)
            bos.write("\r\n".toByteArray())
        }

        writer.append("--$boundary--\r\n")
        writer.flush()
        return bos.toByteArray()
    }

    override fun parseNetworkResponse(response: NetworkResponse): Response<NetworkResponse> {
        return Response.success(response, HttpHeaderParser.parseCacheHeaders(response))
    }

    override fun deliverResponse(response: NetworkResponse) {
        listener.onResponse(response)
    }
}
