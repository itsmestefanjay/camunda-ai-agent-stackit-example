# camunda-ai-agent-stackit-example
Camunda 8 agentic orchestration example: the AI Agent connector on STACKIT AI Model Serving, the OpenAI-compatible LLM service of the German sovereign cloud. A BPMN process, element templates and a Spring Boot worker show how to run AI agents in Camunda while prompts and process data stay in Germany. GDPR-friendly, Java, ready to fork.

A customer support agent for a fictional online shop. The agent looks up customers and orders with two job-worker tools and asks a human of the support team when it is unsure. A human reviews the drafted reply.

<p align="center">
  <img src="docs/images/support-agent-process.png" alt="Support agent process: AI Agent sub-process with the tools Get customer, Get order status and Ask support team, followed by Review reply" width="1000">
</p>

## Requirements

- JDK 21, Maven 3.9
- Docker
- A STACKIT project with AI Model Serving and an API token

## Get a token

```sh
stackit curl -X POST https://model-serving.api.eu01.stackit.cloud/v1/projects/<project-id>/regions/eu01/tokens \
  --data '{"name":"camunda-ai-agent","ttlDuration":"336h"}'
```

The token content is shown once. Copy `.env.example` to `.env` and set `STACKIT_AI_API_KEY`.

## Models

Shared models in eu01 as of 2026-11-11. Specs from [Available shared models](https://docs.stackit.cloud/products/data-and-ai/ai-model-serving/basics/available-shared-models/), prices from the STACKIT portal pricing information (EUR per 1M tokens). The model ID is the value for the `model` variable or `provider.openai.model.model` in the BPMN.

| Model ID                                     | Type      | Tier     | Context  | Max output | Tool calling | Input €/1M | Output €/1M | Note                                      |
|----------------------------------------------|-----------|----------|----------|------------|--------------|------------|-------------|-------------------------------------------|
| `openai/gpt-oss-20b`                         | chat      | Standard | 131K     | 8192       | yes          | 0.15       | 0.25        | default, verified                         |
| `openai/gpt-oss-120b`                        | chat      | Plus     | 131K     | 16384      | yes          | 0.45       | 0.65        |                                           |
| `cortecs/Llama-3.3-70B-Instruct-FP8-Dynamic` | chat      | Plus     | 128K     | 4096       | yes          | 0.45       | 0.65        |                                           |
| `google/gemma-4-31B-it`                      | chat      | Plus     | 256K     | 16384      | yes          | 0.45       | 0.65        | text + image input                        |
| `google/gemma-3-27b-it`                      | chat      | Plus     | –        | –          | –            | 0.45       | 0.65        | served and priced, not in the models docs |
| `Qwen/Qwen3.8-27B`                           | chat      | Plus     | 262K     | 16384      | yes          | 0.45       | 0.65        | text + image input                        |
| `Qwen/Qwen3-VL-235B-A22B-Instruct-FP8`       | chat      | Premium  | 200K     | 16384      | yes          | 1.50       | 1.75        | text + image input                        |

Billing is pay-per-token in units of 1M tokens per day: a day with fewer than 1M tokens is charged as a full 1M. Prices change; check the portal.

List the IDs your token can use:

```sh
curl -s -H "Authorization: Bearer $STACKIT_AI_API_KEY" \
  https://api.openai-compat.model-serving.eu01.onstackit.cloud/v1/models | jq -r '.data[].id'
```

The agent needs a chat model with tool calling. STACKIT recommends retries for tool calling; the agent task has 3 retries.

Switch the model per instance:

```json
{"processDefinitionId":"stackit-support-agent","variables":{"request":"Where is my order 4711?","model":"openai/gpt-oss-120b"}}
```

## Run

```sh
docker compose up -d
mvn spring-boot:run
```

Start an instance:

```sh
curl -X POST http://localhost:8080/v2/process-instances \
  -H 'Content-Type: application/json' \
  -d '{"processDefinitionId":"stackit-support-agent","variables":{"request":"Where is my order 4711?"}}'
```

Open Operate and Tasklist at http://localhost:8080 (demo / demo). The agent may create an `Ask support team` task; afterwards `Review reply` shows the drafted reply.

## Build and test

```sh
mvn verify
```

## How it works

- `Support_Agent` is an AI Agent sub-process (connector v2, `io.camunda.agenticai:aiagent:subprocess:2`) running on the connectors runtime.
- Tools are the root elements of the ad-hoc sub-process, those without incoming flow. The element id is the tool name.
- `<bpmn:documentation>` of a tool is its description for the model.
- `fromAi(toolCall.<param>, "<description>")` in an input mapping declares a tool parameter. The description is a prompt.
- Every tool must produce the variable `toolCallResult`. Job workers return it, the user task maps its output to it.
- `Get_Customer` and `Get_Order_Status` are service tasks served by `ShopTools`. Unknown ids return a "not found" result, not a failure.
- `Ask_Support_Team` is a user task and acts as the human tool.
- `Review_Reply` shows `agent.responseText` to a human before the process ends.

## Configure

Model provider of `Support_Agent` in Camunda Modeler (AI Agent Sub-process template):

<p align="center">
  <img src="docs/images/modeler-model-provider.png" alt="Camunda Modeler model provider panel: OpenAI, Chat Completions, custom endpoint, API key from secrets" width="455">
</p>

| Knob            | Where                                                                                                       | Default                                                           |
|-----------------|-------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------|
| Endpoint        | `provider.openai.backend.custom.endpoint` in `support-agent.bpmn`                                           | `https://api.openai-compat.model-serving.eu01.onstackit.cloud/v1` |
| Model           | process variable `model`                                                                                    | `openai/gpt-oss-20b`                                              |
| Secret name     | `{{secrets.STACKIT_AI_API_KEY}}` in the BPMN, env var `SECRET_STACKIT_AI_API_KEY` on the connectors runtime | `.env` via `docker-compose.yaml`                                  |
| Max model calls | `data.limits.maxModelCalls` in `support-agent.bpmn`                                                         | 10                                                                |

## Production notes

- Provide secrets through STACKIT Secrets Manager, see https://github.com/itsmestefanjay/camunda-connectors-stackit-secrets.
- The token is a bearer token. Rotate it before `ttlDuration` expires.
- Model calls go only to STACKIT eu01; process data stays in your Camunda cluster.
