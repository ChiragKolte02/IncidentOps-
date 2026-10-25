
import httpx

from app.config import settings


class PrometheusClient:

    def __init__(self):
        self.base_url = settings.prometheus_url.rstrip("/")

    async def query(self, promql: str):
        async with httpx.AsyncClient(timeout=10.0) as client:
            response = await client.get(
                f"{self.base_url}/api/v1/query",
                params={"query": promql}
            )

            response.raise_for_status()
            data = response.json()

            if data.get("status") != "success":
                raise ValueError(
                    f"Prometheus query failed: {data.get('error')}"
                )

            return data["data"]["result"]

    async def query_range(
        self,
        promql: str,
        start: str,
        end: str,
        step: str = "30s"
    ):
        async with httpx.AsyncClient(timeout=15.0) as client:
            response = await client.get(
                f"{self.base_url}/api/v1/query_range",
                params={
                    "query": promql,
                    "start": start,
                    "end": end,
                    "step": step
                }
            )

            response.raise_for_status()
            data = response.json()

            if data.get("status") != "success":
                raise ValueError(
                    f"Prometheus range query failed: {data.get('error')}"
                )

            return data["data"]["result"]

    async def get_alerts(self):
        async with httpx.AsyncClient(timeout=10.0) as client:
            response = await client.get(
                f"{self.base_url}/api/v1/alerts"
            )

            response.raise_for_status()
            data = response.json()

            if data.get("status") != "success":
                raise ValueError("Failed to retrieve alerts")

            return data["data"]["alerts"]
