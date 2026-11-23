package com.SaaS.Constructora.documents.application;

/** URL de descarga de corta duración devuelta por el caso de uso. */
public record DownloadLink(String url, long expiresInSeconds) { }
