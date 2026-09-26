package com.eurojet.russiantutor.ai
import java.io.File
object NativeLlama {
    init { System.loadLibrary("russiantutor_native") }
    private external fun generate(modelPath:String,prompt:String,level:String,maxTokens:Int,threads:Int):String
    fun generate(model:LocalModel,file:File,prompt:String,level:String):String {
        val threads=(Runtime.getRuntime().availableProcessors()-1).coerceIn(2,8)
        return generate(file.absolutePath,prompt,level,model.maxTokens,threads)
    }
}