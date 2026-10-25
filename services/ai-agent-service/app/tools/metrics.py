
from app.tools.prometheus import PrometheusClient


prometheus = PrometheusClient()


# Monitored targets from IncidentOps
TARGETS = {
    "unix": {
        "container": "incidentops-unix-server",
        "instance": "unix-server:9100"
    },
    "dba": {
        "container": "incidentops-db-server",
        "instance": "db-server:9100"
    }
}


# ==========================================
# CPU USAGE
# ==========================================

async def get_cpu_usage(team: str):

    target = TARGETS[team]

    query = (
        "100 * sum("
        "rate(container_cpu_usage_seconds_total"
        f'{{name="{target["container"]}"}}[1m])'
        ")"
    )

    return await prometheus.query(query)


# ==========================================
# MEMORY USAGE
# ==========================================

async def get_memory_usage(team: str):

    target = TARGETS[team]

    query = (
        "100 * (1 - "
        f'node_memory_MemAvailable_bytes{{instance="{target["instance"]}"}}'
        " / "
        f'node_memory_MemTotal_bytes{{instance="{target["instance"]}"}}'
        ")"
    )

    return await prometheus.query(query)


# ==========================================
# DISK USAGE
# ==========================================

async def get_disk_usage(team: str):

    target = TARGETS[team]

    query = (
        "100 * (1 - "
        f'node_filesystem_avail_bytes{{instance="{target["instance"]}"}}'
        " / "
        f'node_filesystem_size_bytes{{instance="{target["instance"]}"}}'
        ")"
    )

    return await prometheus.query(query)


# ==========================================
# LOAD AVERAGE
# ==========================================

async def get_load_average(team: str):

    target = TARGETS[team]

    query = (
        f'node_load1{{instance="{target["instance"]}"}}'
    )

    return await prometheus.query(query)
