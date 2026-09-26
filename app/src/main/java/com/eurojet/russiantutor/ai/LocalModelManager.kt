package com.eurojet.russiantutor.ai
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
class LocalModelManager(private val context:Context){ suspend fun download(model:LocalModel,onProgress:(Int)->Unit):Result<File> = withContext(Dispatchers.IO){runCatching{val dir=File(context.filesDir,"models").apply{mkdirs()};val target=File(dir,model.fileName);val temp=File(dir,model.fileName+".part");val c=URL(model.downloadUrl).openConnection() as HttpURLConnection;c.connectTimeout=20000;c.readTimeout=30000;c.connect();if(c.responseCode !in 200..299)error("HTTP "+c.responseCode);val total=c.contentLengthLong;c.inputStream.use{input->temp.outputStream().use{out->val b=ByteArray(65536);var done=0L;while(true){val n=input.read(b);if(n<0)break;out.write(b,0,n);done+=n;if(total>0)onProgress((done*100/total).toInt())}}};if(!temp.renameTo(target))error("Could not finish download");target}}; fun isInstalled(m:LocalModel)=ModelCatalog.file(context,m).exists() }