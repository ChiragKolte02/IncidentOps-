
from datetime import datetime

from pydantic import BaseModel, field_validator


class InvestigationRequest(BaseModel):

    incident_number: str
    alert_name: str
    team: str
    instance: str
    starts_at: datetime

    @field_validator("starts_at")
    @classmethod
    def require_timezone(cls, value: datetime):
        if value.tzinfo is None:
            raise ValueError(
                "starts_at must include a timezone, such as Z or +05:30"
            )
        return value
