#include <jni.h>
#include <android/log.h>
#include <llama.h>
#include <mutex>
#include <string>
#include <vector>
#include <algorithm>

#define LOG_TAG "RussianTutor"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static std::once_flag g_backend_once;
static std::mutex g_model_mutex;
static llama_model * g_model = nullptr;
static std::string g_model_path;

static bool ensure_model(const std::string & path) {
    std::call_once(g_backend_once, [] { llama_backend_init(); });
    std::lock_guard<std::mutex> lock(g_model_mutex);
    if (g_model && g_model_path == path) return true;
    if (g_model) {
        llama_model_free(g_model);
        g_model = nullptr;
        g_model_path.clear();
    }
    llama_model_params mp = llama_model_default_params();
    mp.n_gpu_layers = 0;
    mp.use_mmap = true;
    g_model = llama_model_load_from_file(path.c_str(), mp);
    if (!g_model) {
        LOGE("Could not load GGUF model: %s", path.c_str());
        return false;
    }
    g_model_path = path;
    return true;
}

static std::string apply_chat_template(const llama_model * model, const std::string & user, const std::string & level) {
    const char * tmpl = llama_model_chat_template(model, nullptr);
    std::string system =
        "You are a Russian language tutor. The learner is level " + level +
        ". Reply primarily in Russian. Correct one important mistake briefly when useful. "
        "Keep responses age-appropriate, educational, and concise.";
    llama_chat_message messages[2] = {
        {"system", system.c_str()},
        {"user", user.c_str()}
    };
    std::vector<char> buf(8192);
    if (tmpl) {
        int32_t n = llama_chat_apply_template(tmpl, messages, 2, true, buf.data(), (int32_t)buf.size());
        if (n > 0 && n < (int32_t)buf.size()) return std::string(buf.data(), n);
        if (n > 0) {
            buf.resize((size_t)n + 1);
            n = llama_chat_apply_template(tmpl, messages, 2, true, buf.data(), (int32_t)buf.size());
            if (n > 0) return std::string(buf.data(), n);
        }
    }
    return system + "\n\nUser: " + user + "\nAssistant:";
}

static std::string generate(const std::string & path, const std::string & prompt, const std::string & level, int max_tokens, int threads) {
    if (!ensure_model(path)) return "ERROR: Could not load the selected local model.";

    llama_model * model = g_model;
    llama_context_params cp = llama_context_default_params();
    cp.n_ctx = 2048;
    cp.n_batch = 512;
    cp.n_threads = std::max(1, threads);
    cp.n_threads_batch = std::max(1, threads);
    cp.flash_attn_type = LLAMA_FLASH_ATTN_TYPE_AUTO;

    llama_context * ctx = llama_init_from_model(model, cp);
    if (!ctx) return "ERROR: Could not create inference context.";

    const llama_vocab * vocab = llama_model_get_vocab(model);
    std::string formatted = apply_chat_template(model, prompt, level);

    std::vector<llama_token> tokens(formatted.size() + 512);
    int32_t n = llama_tokenize(vocab, formatted.c_str(), (int32_t)formatted.size(),
                               tokens.data(), (int32_t)tokens.size(), true, true);
    if (n < 0) {
        tokens.resize((size_t)-n);
        n = llama_tokenize(vocab, formatted.c_str(), (int32_t)formatted.size(),
                           tokens.data(), (int32_t)tokens.size(), true, true);
    }
    if (n <= 0 || n > (int32_t)tokens.size() || n >= (int32_t)cp.n_ctx) {
        llama_free(ctx);
        return "ERROR: Prompt is too long for the local context.";
    }
    tokens.resize((size_t)n);

    llama_batch batch = llama_batch_get_one(tokens.data(), n);
    if (llama_decode(ctx, batch) != 0) {
        llama_free(ctx);
        return "ERROR: Model decode failed.";
    }

    llama_sampler_chain_params sp = llama_sampler_chain_default_params();
    llama_sampler * sampler = llama_sampler_chain_init(sp);
    llama_sampler_chain_add(sampler, llama_sampler_init_top_k(40));
    llama_sampler_chain_add(sampler, llama_sampler_init_top_p(0.90f, 1));
    llama_sampler_chain_add(sampler, llama_sampler_init_temp(0.75f));
    llama_sampler_chain_add(sampler, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));

    std::string output;
    output.reserve((size_t)max_tokens * 4);
    int32_t pos = n;
    for (int i = 0; i < max_tokens; ++i) {
        llama_token id = llama_sampler_sample(sampler, ctx, -1);
        if (llama_vocab_is_eog(vocab, id)) break;
        char piece[256];
        int32_t piece_len = llama_token_to_piece(vocab, id, piece, sizeof(piece), 0, true);
        if (piece_len > 0) output.append(piece, piece_len);
        llama_sampler_accept(sampler, id);

        llama_token next = id;
        llama_batch next_batch = llama_batch_get_one(&next, 1);
        if (llama_decode(ctx, next_batch) != 0) break;
        ++pos;
        if (pos >= (int32_t)cp.n_ctx) break;
    }

    llama_sampler_free(sampler);
    llama_free(ctx);
    return output.empty() ? "Я не смог сгенерировать ответ. Попробуй ещё раз." : output;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_eurojet_russiantutor_ai_NativeLlama_generate(
        JNIEnv * env, jobject, jstring modelPath, jstring prompt, jstring level, jint maxTokens, jint threads) {
    const char * path = env->GetStringUTFChars(modelPath, nullptr);
    const char * text = env->GetStringUTFChars(prompt, nullptr);
    const char * lvl = env->GetStringUTFChars(level, nullptr);
    std::string result = generate(path, text, lvl, (int)maxTokens, (int)threads);
    env->ReleaseStringUTFChars(modelPath, path);
    env->ReleaseStringUTFChars(prompt, text);
    env->ReleaseStringUTFChars(level, lvl);
    return env->NewStringUTF(result.c_str());
}
