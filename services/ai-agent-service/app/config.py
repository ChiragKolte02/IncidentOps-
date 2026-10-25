
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = "IncidentOps AI Agent"

    prometheus_url: str = "http://prometheus:9090"

    gemini_api_key: str = ""
    gemini_model: str = "gemini-3.8-flash"

    groq_api_key: str = ""
    groq_model: str = "openai/gpt-oss-120b"

    llm_provider: str = "groq"

    internal_api_key: str

    model_config = SettingsConfigDict(
        env_file=".env",
        extra="ignore"
    )


settings = Settings()
