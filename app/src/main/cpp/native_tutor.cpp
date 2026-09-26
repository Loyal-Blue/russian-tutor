#include <jni.h>
#include <android/log.h>
#include <llama.h>
#include <mutex>
#include <string>
#include <vector>
#include <algorithm>
#define LOG_TAG "RussianTutor"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
static std::once_flag g_once; static std::mutex g_mutex; static llama_model* g_model=nullptr; static std::string g_path;
static bool load_model(const std::string& path){
    std::call_once(g_once,[]{llama_backend_init();}); std::lock_guard<std::mutex> lock(g_mutex);
    if(g_model&&g_path==path)return true; if(g_model){llama_model_free(g_model);g_model=nullptr;g_path.clear();}
    auto p=llama_model_default_params(); p.n_gpu_layers=0; g_model=llama_model_load_from_file(path.c_str(),p);
    if(!g_model){LOGE("Failed to load %s",path.c_str());return false;} g_path=path; return true;
}
static std::string format_prompt(llama_model* model,const std::string& user,const std::string& level){
    std::string system="You are a Russian language tutor. Learner level: "+level+". Reply primarily in Russian. Correct one important mistake briefly when useful. Keep replies concise and educational.";
    const char* tmpl=llama_model_chat_template(model,nullptr); llama_chat_message msgs[2]={{"system",system.c_str()},{"user",user.c_str()}}; std::vector<char> buf(8192);
    if(tmpl){int n=llama_chat_apply_template(tmpl,msgs,2,true,buf.data(),(int)buf.size()); if(n>0){if(n>=(int)buf.size()){buf.resize(n+1);n=llama_chat_apply_template(tmpl,msgs,2,true,buf.data(),(int)buf.size());}if(n>0)return std::string(buf.data(),n);}}
    return system+"\n\nUser: "+user+"\nAssistant:";
}
static std::string run(const std::string& path,const std::string& user,const std::string& level,int max_tokens,int threads){
    if(!load_model(path))return "ERROR: Could not load the local GGUF model."; auto* model=g_model;
    auto cp=llama_context_default_params(); cp.n_ctx=2048; cp.n_batch=512; cp.n_threads=std::max(1,threads); cp.n_threads_batch=std::max(1,threads);
    auto* ctx=llama_init_from_model(model,cp); if(!ctx)return "ERROR: Could not create inference context.";
    const auto* vocab=llama_model_get_vocab(model); std::string prompt=format_prompt(model,user,level); std::vector<llama_token> tok(prompt.size()+512);
    int n=llama_tokenize(vocab,prompt.c_str(),(int)prompt.size(),tok.data(),(int)tok.size(),true,true);
    if(n<0){tok.resize((size_t)-n);n=llama_tokenize(vocab,prompt.c_str(),(int)prompt.size(),tok.data(),(int)tok.size(),true,true);}
    if(n<=0||n>=(int)cp.n_ctx){llama_free(ctx);return "ERROR: Prompt is too long.";}
    if(llama_decode(ctx,llama_batch_get_one(tok.data(),n))!=0){llama_free(ctx);return "ERROR: Prompt decode failed.";}
    auto sp=llama_sampler_chain_default_params(); auto* smp=llama_sampler_chain_init(sp);
    llama_sampler_chain_add(smp,llama_sampler_init_top_k(40)); llama_sampler_chain_add(smp,llama_sampler_init_top_p(0.9f,1)); llama_sampler_chain_add(smp,llama_sampler_init_temp(0.75f)); llama_sampler_chain_add(smp,llama_sampler_init_dist(LLAMA_DEFAULT_SEED));
    std::string out; out.reserve(max_tokens*4);
    for(int i=0;i<max_tokens;i++){llama_token id=llama_sampler_sample(smp,ctx,-1); if(llama_vocab_is_eog(vocab,id))break; char piece[256];int len=llama_token_to_piece(vocab,id,piece,sizeof(piece),0,true);if(len>0)out.append(piece,len);llama_sampler_accept(smp,id);llama_token next=id;if(llama_decode(ctx,llama_batch_get_one(&next,1))!=0)break;}
    llama_sampler_free(smp); llama_free(ctx); return out.empty()?"Я не смог сгенерировать ответ. Попробуй ещё раз.":out;
}
extern "C" JNIEXPORT jstring JNICALL Java_com_eurojet_russiantutor_ai_NativeLlama_generate(JNIEnv* env,jobject,jstring model,jstring prompt,jstring level,jint max,jint threads){
    const char* p=env->GetStringUTFChars(model,nullptr);const char* u=env->GetStringUTFChars(prompt,nullptr);const char* l=env->GetStringUTFChars(level,nullptr);std::string r=run(p,u,l,(int)max,(int)threads);env->ReleaseStringUTFChars(model,p);env->ReleaseStringUTFChars(prompt,u);env->ReleaseStringUTFChars(level,l);return env->NewStringUTF(r.c_str());
}