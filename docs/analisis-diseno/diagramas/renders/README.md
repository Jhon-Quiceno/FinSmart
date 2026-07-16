# Diagramas Renderizados — KoroFin

Esta carpeta contiene los diagramas renderizados en formato SVG, generados a partir de los archivos fuente PlantUML en `../puml/`.

## Archivos SVG

| Archivo                            | Fuente PUML                       | Descripción                                    |
| ---------------------------------- | --------------------------------- | ---------------------------------------------- |
| `arquitectura-general.svg`         | `arquitectura-general.puml`       | Arquitectura general del sistema (capas)       |
| `modelo-datos.svg`                 | `modelo-datos.puml`               | Modelo entidad-relación con 10 colecciones     |
| `flujo-autenticacion.svg`          | `flujo-autenticacion.puml`        | Flujo de autenticación JWT con refresh tokens  |
| `flujo-ia-multiproveedor.svg`      | `flujo-ia-multiproveedor.puml`    | Flujo de IA multi-proveedor con failover       |

## Regenerar renders

Los SVG se generaron mediante el servicio online de PlantUML. Para regenerar localmente:

```bash
# Requiere Java + PlantUML
plantuml ../puml/*.puml -o .
```

> Los SVG son escalables y editables, ideales para documentación técnica.
