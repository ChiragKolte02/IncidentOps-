
import asyncio
import logging
import random

import httpx

from app.config import settings
from app.dto.investigation_report import InvestigationReport
from app.services.investigation_prompt import build_investigation_prompt


logger = logging.getLogger(__name__)


def make_strict_schema(schema: dict) -> dict:
    """
    Convert Pydantic JSON schema into a schema compatible
    with Groq's strict structured JSON output.

    Applies additionalProperties=false to every object,
    including nested definitions.
    """

    def process(node):
        if isinstance(node, dict):

            if node.get("type") == "object":
                node["additionalProperties"] = False

                properties = node.get("properties", {})

                # Strict schemas require all properties.
                node["required"] = list(properties.keys())

            for value in node.values():
                process(value)

        elif isinstance(node, list):
            for item in node:
                process(item)

    process(schema)

    return schema


class GroqService:

    def __init__(self):
        if not settings.groq_api_key:
            raise ValueError(
                "GROQ_API_KEY is not configured"
            )

        self.url = (
            "https://api.groq.com/openai/v1/chat/completions"
        )

    async def generate_investigation(
        self,
        incident: dict,
        evidence: dict
    ) -> InvestigationReport:

        prompt = build_investigation_prompt(
            incident=incident,
            evidence=evidence
        )

        # Generate a Groq-compatible strict schema.
        schema = make_strict_schema(
            InvestigationReport.model_json_schema()
        )

        payload = {
            "model": settings.groq_model,
            "messages": [
                {
                    "role": "system",
                    "content": (
                        "You are an evidence-based SRE "
                        "incident investigation assistant. "
                        "Use only the supplied incident "
                        "context and monitoring evidence. "
                        "Never invent metrics, logs, "
                        "or confirmed root causes. "
                        "Return a JSON investigation report."
                    )
                },
                {
                    "role": "user",
                    "content": prompt
                }
            ],
            "temperature": 0,
            "max_completion_tokens": 2500,
            "response_format": {
                "type": "json_schema",
                "json_schema": {
                    "name": "investigation_report",
                    "strict": True,
                    "schema": schema
                }
            }
        }

        max_attempts = 3

        async with httpx.AsyncClient(
            timeout=httpx.Timeout(
                30.0,
                connect=10.0
            )
        ) as client:

            for attempt in range(1, max_attempts + 1):

                try:
                    logger.info(
                        "Starting Groq investigation attempt %s/%s",
                        attempt,
                        max_attempts
                    )

                    response = await client.post(
                        self.url,
                        headers={
                            "Authorization": (
                                f"Bearer {settings.groq_api_key}"
                            )
                        },
                        json=payload
                    )

                    response.raise_for_status()

                    data = response.json()

                    content = (
                        data["choices"][0]["message"]["content"]
                    )

                    if not content:
                        raise ValueError(
                            "Groq returned an empty report"
                        )

                    report = (
                        InvestigationReport.model_validate_json(
                            content
                        )
                    )

                    logger.info(
                        "Groq investigation completed successfully"
                    )

                    return report

                except httpx.HTTPStatusError as error:
                    status = error.response.status_code

                    if status not in (500, 502, 503, 504):
                        logger.error(
                            "Groq returned non-retryable HTTP %s: %s",
                            status,
                            error.response.text[:1000]
                        )
                        raise

                    logger.warning(
                        "Groq HTTP %s on attempt %s/%s",
                        status,
                        attempt,
                        max_attempts
                    )

                    if attempt == max_attempts:
                        raise

                except (
                    httpx.TimeoutException,
                    httpx.TransportError
                ) as error:
                    logger.warning(
                        "Groq connection error on attempt %s/%s: %s",
                        attempt,
                        max_attempts,
                        type(error).__name__
                    )

                    if attempt == max_attempts:
                        raise

                delay = (
                    2 ** (attempt - 1)
                    + random.uniform(0, 0.5)
                )

                logger.info(
                    "Retrying Groq in %.1f seconds",
                    delay
                )

                await asyncio.sleep(delay)

        raise RuntimeError(
            "Groq investigation failed after all attempts"
        )
