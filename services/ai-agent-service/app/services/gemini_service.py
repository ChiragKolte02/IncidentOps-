
import asyncio
import logging
import random

from google import genai
from google.genai import types
from google.genai.errors import ServerError

from app.config import settings
from app.dto.investigation_report import InvestigationReport
from app.services.investigation_prompt import build_investigation_prompt


logger = logging.getLogger(__name__)


class GeminiService:

    def __init__(self):
        if not settings.gemini_api_key:
            raise ValueError(
                "GEMINI_API_KEY is not configured"
            )

        self.client = genai.Client(
            api_key=settings.gemini_api_key
        )

    async def test_connection(self):
        """
        Test connectivity to the Gemini API.
        """

        response = await asyncio.wait_for(
            self.client.aio.models.generate_content(
                model=settings.gemini_model,
                contents=(
                    "You are the IncidentOps AI investigation "
                    "assistant. Briefly explain your role "
                    "in one sentence."
                )
            ),
            timeout=30
        )

        return response.text

    async def generate_investigation(
        self,
        incident: dict,
        evidence: dict
    ) -> InvestigationReport:
        """
        Generate an evidence-based incident investigation.

        Includes:
        - Structured JSON output
        - Pydantic validation
        - 30-second timeout per attempt
        - Retries for temporary Gemini server errors
        - Retries for request timeouts
        """

        prompt = build_investigation_prompt(
            incident=incident,
            evidence=evidence
        )

        max_attempts = 3
        timeout_seconds = 30

        for attempt in range(1, max_attempts + 1):

            try:
                logger.info(
                    "Starting Gemini investigation attempt %s/%s",
                    attempt,
                    max_attempts
                )

                response = await asyncio.wait_for(
                    self.client.aio.models.generate_content(
                        model=settings.gemini_model,
                        contents=prompt,
                        config=types.GenerateContentConfig(
                            temperature=0.1,
                            response_mime_type="application/json",
                            response_schema=InvestigationReport
                        )
                    ),
                    timeout=timeout_seconds
                )

                if not response.text:
                    raise ValueError(
                        "Gemini returned an empty investigation response"
                    )

                report = InvestigationReport.model_validate_json(
                    response.text
                )

                logger.info(
                    "Gemini investigation completed successfully"
                )

                return report

            except asyncio.TimeoutError:

                logger.warning(
                    "Gemini request timed out on attempt %s/%s",
                    attempt,
                    max_attempts
                )

                if attempt == max_attempts:
                    logger.error(
                        "Gemini investigation failed after all "
                        "timeout retries"
                    )
                    raise

            except ServerError as error:

                status_code = getattr(
                    error,
                    "code",
                    None
                )

                if status_code not in (
                    500,
                    502,
                    503,
                    504
                ):
                    raise

                logger.warning(
                    "Gemini server error HTTP %s on attempt %s/%s",
                    status_code,
                    attempt,
                    max_attempts
                )

                if attempt == max_attempts:
                    logger.error(
                        "Gemini investigation failed after "
                        "all server-error retries"
                    )
                    raise

            # Wait before retrying.
            delay = (2 ** (attempt - 1)) + random.uniform(
                0,
                1
            )

            logger.info(
                "Retrying Gemini in %.1f seconds",
                delay
            )

            await asyncio.sleep(delay)

        raise RuntimeError(
            "Gemini investigation failed after all attempts"
        )
