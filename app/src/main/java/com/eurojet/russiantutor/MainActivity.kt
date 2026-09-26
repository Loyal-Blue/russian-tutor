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
import androidx.compose.ui.unit.dp
import com.eurojet.russiantutor.ai.*
import com.eurojet.russiantutor.data.Curriculum
import kotlinx.coroutines.launch
class MainActivity:ComponentActivity(){override fun onCreate(b:Bundle?){super.onCreate(b);setContent{App()}}}
@Composable fun App(){var tab by remember{mutableIntStateOf(0)};Scaffold(bottomBar={NavigationBar{listOf("Learn","Practice","AI Lab").forEachIndexed{i,s->NavigationBarItem(tab==i,{tab=i},{Text(s)})}}}){p->Box(Modifier.padding(p)){when(tab){0->Learn();1->Practice();2->Lab()}}}}
@Composable fun Learn(){var done by remember{mutableStateOf(setOf<Int>())};LazyColumn(Modifier.padding(16.dp)){item{Text("РУССКИЙ",style=MaterialTheme.typography.headlineLarge);Text("Your step-by-step Russian tutor");Spacer(Modifier.height(16.dp));LinearProgressIndicator({done.size.toFloat()/Curriculum.lessons.size},Modifier.fillMaxWidth());Spacer(Modifier.height(16.dp))};items(Curriculum.lessons){l->Card(Modifier.fillMaxWidth().padding(vertical=5.dp)){Column(Modifier.padding(16.dp)){Text(l.id.toString()+"  "+l.title,style=MaterialTheme.typography.titleLarge);Text(l.level+" · "+l.explanation);Button({done=done+l.id}){Text(if(l.id in done)"Completed" else "Start lesson")}}}}}}
@Composable fun Practice(){val c=androidx.compose.ui.platform.LocalContext.current;val m=ModelCatalog.recommended(c);val ai=remember(m){LocalTutor(m)};val scope=rememberCoroutineScope();var input by remember{mutableStateOf("")};var answer by remember{mutableStateOf("Привет! Расскажи о своём дне по-русски.")};Column(Modifier.fillMaxSize().padding(16.dp)){Text("AI conversation",style=MaterialTheme.typography.headlineMedium);Text("Offline model: "+m.name);Spacer(Modifier.height(12.dp));Text(answer,Modifier.weight(1f));Row{OutlinedTextField(input,{input=it},Modifier.weight(1f),placeholder={Text("Write in Russian…")});Button({val x=input;input="";scope.launch{answer=ai.chat(x,"A1")}}){Text("Send")}}}}
@Composable fun Lab(){val c=androidx.compose.ui.platform.LocalContext.current;val ai=remember{LocalTutor(ModelCatalog.recommended(c))};val scope=rememberCoroutineScope();var topic by remember{mutableStateOf("")};var result by remember{mutableStateOf("")};Column(Modifier.fillMaxSize().padding(16.dp)){Text("AI lesson lab",style=MaterialTheme.typography.headlineMedium);Text("Generate a lesson from any topic.");OutlinedTextField(topic,{topic=it},Modifier.fillMaxWidth(),placeholder={Text("travel, trains, school…")});Button({scope.launch{result=ai.generateLesson(if(topic.isBlank())"Everyday Russian" else topic,"A1")}}){Text("Generate lesson")};Spacer(Modifier.height(16.dp));Text(result)}}