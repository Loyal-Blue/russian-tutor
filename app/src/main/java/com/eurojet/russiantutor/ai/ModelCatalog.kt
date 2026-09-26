package com.eurojet.russiantutor.ai
import android.content.Context
import android.os.StatFs
import java.io.File
data class LocalModel(val id:String,val name:String,val subtitle:String,val sizeLabel:String,val minRamGb:Int,val params:String,val repoUrl:String,val downloadUrl:String,val fileName:String,val maxTokens:Int)
object ModelCatalog {
    val models=listOf(
        LocalModel("swift","Swift","Fast & tiny","~491 MB",3,"0.5B","https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF","https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf","qwen2.5-0.5b-instruct-q4_k_m.gguf",160),
        LocalModel("balanced","Balanced","Everyday tutor","~806 MB",4,"1B","https://huggingface.co/ggml-org/gemma-3-1b-it-GGUF","https://huggingface.co/ggml-org/gemma-3-1b-it-GGUF/resolve/main/gemma-3-1b-it-Q4_K_M.gguf","gemma-3-1b-it-Q4_K_M.gguf",192),
        LocalModel("deep","Deep Tutor","More capable","~2 GB",6,"3B","https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF","https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q4_K_M.gguf","Llama-3.2-3B-Instruct-Q4_K_M.gguf",256)
    )
    fun recommended(c:Context):LocalModel {
        val ram=Runtime.getRuntime().maxMemory()/(1024*1024*1024)
        val free=StatFs(c.filesDir.path).availableBytes/(1024*1024*1024)
        return if(ram>=6&&free>=3)models[2] else if(ram>=4&&free>=2)models[1] else models[0]
    }
    fun file(c:Context,m:LocalModel)=File(File(c.filesDir,"models"),m.fileName)
}