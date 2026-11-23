package com.SaaS.Constructora.documents.application;

import com.SaaS.Constructora.documents.domain.AssetKind;

/** Datos estructurales de la primera versión de un documento. */
public record UploadDocumentCommand(String title, AssetKind kind, String versionLabel) { }
