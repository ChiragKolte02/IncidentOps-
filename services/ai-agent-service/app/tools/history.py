
from datetime import datetime, timedelta, timezone

from app.tools.prometheus import PrometheusClient
from app.tools.metrics import TARGETS


prometheus = PrometheusClient()


def build_queries(team: str):
    """
    Build Prometheus queries for the selected monitored server.
    """

    if team not in TARGETS:
        raise ValueError("Unsupported monitoring team")

    target = TARGETS[team]

    container = target["container"]
    instance = target["instance"]

    queries = {
        # Match the existing IncidentOps CPU alert normalization.
        "cpu_percent": (
            "100 * sum("
            "rate(container_cpu_usage_seconds_total"
            f'{{name="{container}"}}[1m])'
            ") / 12"
        ),

        "memory_percent": (
            "100 * (1 - "
            f'node_memory_MemAvailable_bytes{{instance="{instance}"}}'
            " / "
            f'node_memory_MemTotal_bytes{{instance="{instance}"}}'
            ")"
        ),

        "disk_percent": (
            "100 * (1 - "
            f'node_filesystem_avail_bytes{{instance="{instance}",fstype!~"tmpfs|overlay"}}'
            " / "
            f'node_filesystem_size_bytes{{instance="{instance}",fstype!~"tmpfs|overlay"}}'
            ")"
        ),

        "load_average": (
            f'node_load1{{instance="{instance}"}}'
        )
    }

    return queries


async def get_historical_metrics(
    team: str,
    incident_time: datetime
):
    """
    Retrieve historical metrics around an incident.

    Window:
      15 minutes before incident
      15 minutes after incident

    Prometheus step:
      30 seconds
    """

    if incident_time.tzinfo is None:
        raise ValueError(
            "Incident timestamp must include a timezone"
        )

    incident_time = incident_time.astimezone(timezone.utc)

    start = incident_time - timedelta(minutes=15)
    end = incident_time + timedelta(minutes=15)

    # Do not request samples from the future.
    now = datetime.now(timezone.utc)
    end = min(end, now)

    if start > end:
        raise ValueError("Incident time is in the future")

    queries = build_queries(team)

    metrics = {}

    for metric_name, promql in queries.items():
        result = await prometheus.query_range(
            promql=promql,
            start=start.isoformat(),
            end=end.isoformat(),
            step="30s"
        )

        metrics[metric_name] = result

    return {
        "team": team,
        "incident_time": incident_time.isoformat(),
        "window_start": start.isoformat(),
        "window_end": end.isoformat(),
        "step": "30s",
        "metrics": metrics
    }
