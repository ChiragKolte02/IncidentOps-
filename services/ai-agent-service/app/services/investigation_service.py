
from datetime import datetime, timezone
from statistics import mean
from typing import Any
import math


def summarize_series(series: dict) -> dict:
    """
    Summarize one Prometheus time series.

    Prometheus range-vector format:
    {
        "metric": {"instance": "..."},
        "values": [[timestamp, "value"], ...]
    }
    """

    observations = []

    for sample in series.get("values", []):
        if len(sample) != 2:
            continue

        timestamp, raw_value = sample

        try:
            value = float(raw_value)
            timestamp = float(timestamp)
        except (TypeError, ValueError):
            continue

        # Ignore NaN and infinite values.
        if not math.isfinite(value) or not math.isfinite(timestamp):
            continue

        observations.append({
            "timestamp": datetime.fromtimestamp(
                timestamp,
                tz=timezone.utc
            ).isoformat(),
            "value": round(value, 4),
            "_raw_value": value,
            "_raw_timestamp": timestamp
        })

    observations.sort(key=lambda item: item["_raw_timestamp"])

    if not observations:
        return {
            "labels": series.get("metric", {}),
            "sample_count": 0,
            "minimum": None,
            "maximum": None,
            "average": None,
            "first": None,
            "last": None
        }

    raw_values = [
        observation["_raw_value"]
        for observation in observations
    ]

    first = observations[0]
    last = observations[-1]

    return {
        "labels": series.get("metric", {}),
        "sample_count": len(observations),
        "minimum": round(min(raw_values), 4),
        "maximum": round(max(raw_values), 4),
        "average": round(mean(raw_values), 4),
        "first": {
            "timestamp": first["timestamp"],
            "value": first["value"]
        },
        "last": {
            "timestamp": last["timestamp"],
            "value": last["value"]
        }
    }


def summarize_history(history: dict) -> dict[str, Any]:
    """
    Convert historical Prometheus range-vector data
    into a compact evidence summary.

    Each filesystem or other labeled series is
    summarized separately.
    """

    summarized_metrics = {}

    for metric_name, series_list in history.get(
        "metrics", {}
    ).items():

        summarized_metrics[metric_name] = [
            summarize_series(series)
            for series in series_list
        ]

    return {
        "team": history.get("team"),
        "incident_time": history.get("incident_time"),
        "window_start": history.get("window_start"),
        "window_end": history.get("window_end"),
        "step": history.get("step"),
        "metrics": summarized_metrics
    }
