# Running Large Language Models

## Introduction

Welcome to this hands-on lab on running Large Language Models (LLMs)! In this lab, you'll learn how to manage and run LLMs both locally and through cloud services. You'll gain practical experience with tools like Ollama and Podman AI for local model management, as well as explore cloud-based LLM services from Groq and Mistral AI.

## Prerequisites

Before starting the lab, please ensure you have the following installed:

* [httpie](https://httpie.io/cli) for making API calls.

## Learning Goals

By the end of this lab, you will be able to:

* Manage and run LLMs locally using Ollama
* Manage and run LLMs locally using Podman AI
* Consume LLMs from Groq's cloud service
* Consume LLMs from Mistral AI's cloud service

## Exercises

### 1. Managing and Running LLMs locally with Ollama

Ollama is a convenient tool to manage and run large language models. Think of it as the Docker for LLMs - just as Docker manages containers, Ollama manages models. With Ollama, you can access a wide library of models to run on your local infrastructure or in the cloud, maintaining full control of your data and ensuring privacy.

#### 1.1 Install Ollama

* Visit https://ollama.com/download
* Download and install the appropriate version for your operating system.
* Verify the installation by running the following command in your terminal:

```shell
ollama --version
```

#### 1.2 Run your first model

* Open a terminal and run the following command to download the Mistral model:

```shell
ollama pull mistral
```

* This will download and prepare the open-source Mistral model for use.
* Once the download is complete, you can run the model and start chatting with it via the interactive console in your Terminal:

```shell
ollama run mistral
```

* Try asking the model a question, for example: "Why is a raven like a writing desk?"
* Ollama supports running multiple models simultaneously. You can monitor all the models loaded in memory and check whether they are running on CPU or GPU:

```shell
ollama ps
```

* When you're done chatting with the model, exit the interactive console by typing:

```shell
/bye
```

#### 1.3 Try different models

* You can list all the available models with the following command:

```shell
ollama ls
```

* Try pulling a different model. For example, `qwen2.5` is a high-quality model from Alibaba:

```shell
ollama pull qwen2.5
```

* The Ollama library includes both open-source and free models. You can get information about a model's license, architecture, and system prompt as follows:

```shell
ollama show qwen2.5
```

* When you're done using a model, you can remove it to free up space:

```shell
ollama rm qwen2.5
```

### 2. Managing and Running LLMs locally with Podman AI

Podman AI is another tool for running AI models locally, offering a graphical interface for managing and interacting with various LLMs. It's an extension to Podman Desktop, an open-source solution for managing and running containers.

#### 2.1 Install Podman Desktop

* Visit https://podman-desktop.io/docs/installation and follow the instructions to install Podman Desktop for your operating system.
* Verify the installation by opening Podman Desktop.

#### 2.2 Set up Podman AI

* Visit https://podman-desktop.io/docs/ai-lab/installing and follow the instructions to install the Podman AI extension.
* Verify that the setup is complete by checking for the AI Lab section in Podman Desktop.

#### 2.3 Run your first model

* In Podman Desktop, navigate to the AI Lab section and go to _Models > Catalog_.
* Browse the available models and select one to download, for example `instructlab/granite-7b-lab-GGUF`.
* Once downloaded, go to _Models > Services > New Model Service_.
* Select the model you have previously downloaded and click _Create service_.
* After the model is loaded, Podman AI will provide curl instructions to call the model.
* Try asking the model a question, such as: "Why is a raven like a writing desk?"

### 3. Consuming LLMs from Groq

Groq provides access to high-performance LLMs through their cloud API, offering an OpenAI-compatible interface. This allows you to leverage powerful models without the need for local hardware resources.

#### 3.1 Create a Groq account

* Visit https://console.groq.com/
* Sign up for a new account (you can sign up with your GitHub account for a quick setup).
* Choose the "Free" plan, which gives you access to the Groq APIs with low rate limits at no cost.

#### 3.2 Get API credentials

* In the Groq console, navigate to _API Keys_.
* Generate a new API key.
* Copy and securely store your API key on your laptop. For example, set it as an environment variable:

```shell
export GROQ_API_KEY=<your-api-key>
```

* If you plan to use Groq with the Spring AI OpenAI integration, also store the API key as follows:

```shell
export SPRING_AI_OPENAI_API_KEY=${GROQ_API_KEY}
```

#### 3.3 Make your first API call

* Open a Terminal window and use [httpie](https://httpie.io/cli) to make your first API call to Groq and interact with the `llama3.1` model:

```shell
http POST https://api.groq.com/openai/v1/chat/completions \
  Authorization:"Bearer $GROQ_API_KEY" \
  messages:='[{"role": "user", "content": "Why is a raven like a writing desk?"}]' \
  model=llama-3.1-8b-instant
```

### 4. Consuming LLMs from Mistral AI

Mistral AI provides access to their open-source and proprietary LLMs through their cloud API, offering another option for leveraging powerful language models.

#### 4.1 Create a Mistral AI account

* Visit https://console.mistral.ai/
* Sign up for a new account.
* Choose the "Experiment" plan, which gives you access to the Mistral APIs for free.

#### 4.2 Get API credentials

* In the Mistral AI console, navigate to _API Keys_.
* Generate a new API key.
* Copy and securely store your API key on your laptop. For example, set it as an environment variable:

```shell
export MISTRAL_AI_API_KEY=<your-api-key>
```

* If you plan to use Mistral AI with the Spring AI Mistral AI integration, also store the API key as follows:

```shell
export SPRING_AI_MISTRALAI_API_KEY=${MISTRAL_AI_API_KEY}
```

#### 4.3 Make your first API call

* Open a Terminal window and use [httpie](https://httpie.io/cli) to make your first API call to Mistral AI and interact with the `mistral-small` model:

```shell
http POST https://api.mistral.ai/v1/chat/completions \
  Authorization:"Bearer $MISTRAL_AI_API_KEY" \
  messages:='[{"role": "user", "content": "Why is a raven like a writing desk?"}]' \
  model=mistral-small-latest
```

## Conclusion

Congratulations! You've completed the lab on running Large Language Models. You've learned how to manage and run LLMs locally using Ollama and Podman AI, as well as how to consume cloud-based LLM services from Groq and Mistral AI. These skills provide a solid foundation for integrating AI capabilities into your projects and applications.
