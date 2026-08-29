resource "kind_cluster" "jacafi" {
  name            = var.cluster_name
  wait_for_ready  = true
  kubeconfig_path = "${path.module}/kubeconfig"

  kind_config {
    kind        = "Cluster"
    api_version = "kind.x-k8s.io/v1alpha4"

    node {
      role = "control-plane"
    }

    node {
      role = "worker"
    }

    node {
      role = "worker"
    }
  }
}

provider "kubernetes" {
  host                   = kind_cluster.jacafi.endpoint
  client_certificate     = kind_cluster.jacafi.client_certificate
  client_key             = kind_cluster.jacafi.client_key
  cluster_ca_certificate = kind_cluster.jacafi.cluster_ca_certificate
}

resource "kubernetes_namespace_v1" "jacafi" {
  metadata {
    name = "jacafi"
  }
}

resource "kubernetes_secret_v1" "tech_secrets" {
  metadata {
    name      = "tech-secrets"
    namespace = kubernetes_namespace_v1.jacafi.metadata[0].name
  }

  data = {
    DB_USERNAME = var.database_username
    DB_PASSWORD = var.database_password
    JWT_SECRET  = var.jwt_secret
  }
}

resource "kubernetes_persistent_volume_claim_v1" "postgres_data" {
  metadata {
    name      = "postgres-data"
    namespace = kubernetes_namespace_v1.jacafi.metadata[0].name
  }

  spec {
    access_modes = ["ReadWriteOnce"]

    resources {
      requests = {
        storage = "1Gi"
      }
    }
  }
}

resource "kubernetes_deployment_v1" "postgres" {
  metadata {
    name      = "postgres"
    namespace = kubernetes_namespace_v1.jacafi.metadata[0].name
  }

  spec {
    replicas = 1

    selector {
      match_labels = {
        app = "postgres"
      }
    }

    template {
      metadata {
        labels = {
          app = "postgres"
        }
      }

      spec {
        container {
          name  = "postgres"
          image = "postgres:16-alpine"

          env {
            name  = "POSTGRES_DB"
            value = var.database_name
          }

          dynamic "env" {
            for_each = {
              POSTGRES_USER     = "DB_USERNAME"
              POSTGRES_PASSWORD = "DB_PASSWORD"
            }

            content {
              name = env.key

              value_from {
                secret_key_ref {
                  name = kubernetes_secret_v1.tech_secrets.metadata[0].name
                  key  = env.value
                }
              }
            }
          }

          port {
            container_port = 5432
          }

          resources {
            requests = {
              cpu    = "250m"
              memory = "256Mi"
            }
            limits = {
              cpu    = "500m"
              memory = "512Mi"
            }
          }

          readiness_probe {
            exec {
              command = ["sh", "-c", "pg_isready -U $POSTGRES_USER -d $POSTGRES_DB"]
            }
            initial_delay_seconds = 5
            period_seconds        = 10
          }

          volume_mount {
            name       = "postgres-data"
            mount_path = "/var/lib/postgresql/data"
          }
        }

        volume {
          name = "postgres-data"

          persistent_volume_claim {
            claim_name = kubernetes_persistent_volume_claim_v1.postgres_data.metadata[0].name
          }
        }
      }
    }
  }
}

resource "kubernetes_service_v1" "postgres" {
  metadata {
    name      = "postgres"
    namespace = kubernetes_namespace_v1.jacafi.metadata[0].name
  }

  spec {
    selector = {
      app = "postgres"
    }

    port {
      name        = "postgres"
      port        = 5432
      target_port = 5432
    }
  }
}
