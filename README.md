# camunda-ai-agent-stackit-example
Camunda 8 agentic orchestration example: the AI Agent connector on STACKIT AI Model Serving, the OpenAI-compatible LLM service of the German sovereign cloud. A BPMN process, element templates and a Spring Boot worker show how to run AI agents in Camunda while prompts and process data stay in Germany. GDPR-friendly, Java, ready to fork.

> Use at your own risk. Not affiliated with Camunda or STACKIT.

A customer support agent for a fictional online shop. The agent looks up customers and orders with two job-worker tools and asks a human of the support team when it is unsure. A human reviews the drafted reply.

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

Available in eu01 as of 2026-11. The model ID is the value for the `model` variable or `provider.openai.model.model` in the BPMN. List the current IDs with:

```sh
curl -s -H "Authorization: Bearer $STACKIT_AI_API_KEY" \
  https://api.openai-compat.model-serving.eu01.onstackit.cloud/v1/models | jq -r '.data[].id'
```

| Model ID | Type | Tier | Note |
| --- | --- | --- | --- |
| `openai/gpt-oss-20b` | chat | standard | default, verified |
| `Qwen/Qwen3.6-27B` | chat | plus |  |
| `cortecs/Llama-3.3-70B-Instruct-FP8-Dynamic` | chat | plus |  |
| `openai/gpt-oss-120b` | chat | plus |  |
| `Qwen/Qwen3.8-27B` | chat | plus |  |
| `google/gemma-3-27b-it` | chat | plus |  |
| `google/gemma-4-31B-it` | chat | plus |  |
| `Qwen/Qwen3-VL-235B-A22B-Instruct-FP8` | chat | premium |  |
| `intfloat/e5-mistral-7b-instruct` | embedding | standard | not usable for the agent |
| `Qwen/Qwen3-VL-Embedding-8B` | embedding | plus | not usable for the agent |

The agent needs a chat model with tool calling. Only `openai/gpt-oss-20b` is verified by this repo.

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

| Knob | Where | Default |
| --- | --- | --- |
| Endpoint | `provider.openai.backend.custom.endpoint` in `support-agent.bpmn` | `https://api.openai-compat.model-serving.eu01.onstackit.cloud/v1` |
| Model | process variable `model` | `openai/gpt-oss-20b` |
| Secret name | `{{secrets.STACKIT_AI_API_KEY}}` in the BPMN, env var `SECRET_STACKIT_AI_API_KEY` on the connectors runtime | `.env` via `docker-compose.yaml` |
| Max model calls | `data.limits.maxModelCalls` in `support-agent.bpmn` | 10 |

## Production notes

- Provide secrets through STACKIT Secrets Manager, see https://github.com/itsmestefanjay/camunda-connectors-stackit-secrets.
- The token is a bearer token. Rotate it before `ttlDuration` expires.
- Model calls go only to STACKIT eu01; process data stays in your Camunda cluster.

## License

Apache-2.0, see [LICENSE](LICENSE).
