
from typing import Literal

from pydantic import BaseModel


class Observation(BaseModel):
    metric: str
    finding: str


class PossibleCause(BaseModel):
    cause: str
    confidence: Literal["low", "medium", "high"]
    reason: str


class RecommendedAction(BaseModel):
    action: str
    reason: str


class InvestigationReport(BaseModel):
    summary: str
    observations: list[Observation]
    possible_causes: list[PossibleCause]
    root_cause_confirmed: bool
    recommended_actions: list[RecommendedAction]
    limitations: list[str]
