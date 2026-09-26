package com.eurojet.russiantutor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.eurojet.russiantutor.ai.*
import com.eurojet.russiantutor.data.Curriculum
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity:ComponentActivity(){override fun onCreate(b:Bundle?){super.onCreate(b);setContent{App()}}}

@Composable fun App(){
    var tab by remember{mutableIntStateOf(0)}
    Scaffold(bottomBar={NavigationBar{listOf("Learn","Practice","AI Lab").forEachIndexed{i,s->NavigationBarItem(tab==i,{tab=i},{Text(s)})}}}){p->Box(Modifier.padding(p)){when(tab){0->Learn();1->Practice();2->Lab()}}}
}

@Composable fun Learn(){
    var done by remember{mutableStateOf(setOf<Int>())}
    LazyColumn(Modifier.padding(16.dp)){
        item{Text("РУССКИЙ",style=MaterialTheme.typography.headlineLarge);Text("Step-by-step Russian tutor");Spacer(Modifier.height(16.dp));LinearProgressIndicator({done.size.toFloat()/Curriculum.lessons.size},Modifier.fillMaxWidth());Spacer(Modifier.height(16.dp))}
        items(Curriculum.lessons){l->Card(Modifier.fillMaxWidth().padding(vertical=5.dp)){Column(Modifier.padding(16.dp)){Text(l.id.toString()+"  "+l.title,style=MaterialTheme.typography.titleLarge);Text(l.level+" · "+l.explanation);Button({done=done+l.id}){Text(if(l.id in done)"Completed" else "Start lesson")}}}}
    }
}

@Composable fun ModelDownloadCard(model:LocalModel,onReady:(Boolean)->Unit){
    val context=LocalContext.current
    val manager=remember{LocalModelManager(context)}
    var progress by remember{mutableIntStateOf(0)}
    var busy by remember{mutableStateOf(false)}
    val installed=manager.isInstalled(model)
    Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){
        Text(model.name,style=MaterialTheme.typography.titleLarge)
        Text(model.subtitle+" · "+model.sizeLabel+" · "+model.params)
        if(busy)LinearProgressIndicator(progress/100f,Modifier.fillMaxWidth())
        Button(enabled=!busy&&!installed,onClick={
            busy=true
            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO){
                manager.download(model){progress=it}
                withContext(Dispatchers.Main){busy=false;onReady(manager.isInstalled(model))}
            }
        }){Text(if(installed)"Downloaded" else if(busy)"Downloading $progress%" else "Download model")}
    }}
}

@Composable fun Practice(){
    val c=LocalContext.current
    val m=ModelCatalog.recommended(c)
    val file=ModelCatalog.file(c,m)
    val ready=file.exists()
    val scope=rememberCoroutineScope()
    var input by remember{mutableStateOf("")}
    var answer by remember{mutableStateOf(if(ready)"Привет! Расскажи о своём дне по-русски." else "Download a local model to start offline AI.")}

    Column(Modifier.fillMaxSize().padding(16.dp)){
        Text("AI conversation",style=MaterialTheme.typography.headlineMedium)
        Text("Recommended local model: "+m.name)
        if(!ready){ModelDownloadCard(m){answer=if(it)"Привет! Расскажи о своём дне по-русски." else "Model download failed."}}
        Spacer(Modifier.height(12.dp))
        Text(answer,Modifier.weight(1f))
        Row{
            OutlinedTextField(input,{input=it},Modifier.weight(1f),placeholder={Text("Write in Russian…")})
            Button(enabled=ready&&input.isNotBlank(),onClick={val x=input;input="";scope.launch{answer=withContext(Dispatchers.IO){LocalTutor(m){file}.chat(x,"A1")}}}){Text("Send")}
        }
    }
}

@Composable fun Lab(){
    val c=LocalContext.current
    val m=ModelCatalog.recommended(c)
    val file=ModelCatalog.file(c,m)
    val ready=file.exists()
    val scope=rememberCoroutineScope()
    var topic by remember{mutableStateOf("")}
    var result by remember{mutableStateOf(if(ready)"Enter a topic and generate a lesson." else "Download the recommended local model first.")}
    Column(Modifier.fillMaxSize().padding(16.dp)){
        Text("AI lesson lab",style=MaterialTheme.typography.headlineMedium)
        if(!ready)ModelDownloadCard(m){if(it)result="Enter a topic and generate a lesson."}
        OutlinedTextField(topic,{topic=it},Modifier.fillMaxWidth(),placeholder={Text("travel, trains, school…")})
        Button(enabled=ready,onClick={scope.launch{result=withContext(Dispatchers.IO){LocalTutor(m){file}.generateLesson(if(topic.isBlank())"Everyday Russian" else topic,"A1")}}}){Text("Generate lesson")}
        Spacer(Modifier.height(16.dp));Text(result)
    }
}