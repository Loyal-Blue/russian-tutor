package com.eurojet.russiantutor.ai
interface AiTutor {
    suspend fun chat(userMessage:String,level:String):String
    suspend fun generateLesson(topic:String,level:String):String
}
class LocalTutor(private val model:LocalModel,private val modelFileProvider:()->java.io.File?):AiTutor {
    override suspend fun chat(userMessage:String,level:String):String {
        val file=modelFileProvider() ?: return "Download the selected local AI model first."
        return NativeLlama.generate(model,file,userMessage,level)
    }
    override suspend fun generateLesson(topic:String,level:String):String {
        val file=modelFileProvider() ?: return "Download the selected local AI model first."
        val prompt = "Create a short Russian-learning lesson about $topic for level $level. Include 5 useful words with English meanings, one grammar point, a mini dialogue, 3 exercises, and one speaking prompt."
        return NativeLlama.generate(model,file,prompt,level)
    }
}