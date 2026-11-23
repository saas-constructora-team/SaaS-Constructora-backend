package com.SaaS.Constructora.documents.domain;

/**
 * Tipo de archivo técnico admitido en un proyecto (HU-04).
 * Cada tipo tiene su propia política de formatos y tamaño en {@link FilePolicy}.
 */
public enum AssetKind {
    IMAGE,
    DOCUMENT,
    RENDER_3D
}
