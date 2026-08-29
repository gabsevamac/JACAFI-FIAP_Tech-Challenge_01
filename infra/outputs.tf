output "cluster_name" {
  value = kind_cluster.jacafi.name
}

output "kubeconfig_path" {
  value = kind_cluster.jacafi.kubeconfig_path
}
