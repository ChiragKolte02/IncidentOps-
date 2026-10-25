
import secrets

from fastapi import HTTPException, Security, status
from fastapi.security import APIKeyHeader

from app.config import settings


internal_api_key_header = APIKeyHeader(
    name="X-Internal-API-Key",
    auto_error=False
)


async def verify_internal_api_key(
    provided_key: str | None = Security(internal_api_key_header)
):
    if not provided_key or not secrets.compare_digest(
        provided_key,
        settings.internal_api_key
    ):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or missing internal API key"
        )
