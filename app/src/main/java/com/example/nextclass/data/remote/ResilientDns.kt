package com.example.nextclass.data.remote

import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * Uses the phone's DNS first. If that cannot find a host (Private DNS, ad blockers, a flaky ISP
 * resolver), it asks Google and Cloudflare over HTTPS instead.
 */
object ResilientDns : Dns {

    private val backups: List<Dns> by lazy {
        val bootstrap = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
        listOf(
            DnsOverHttps.Builder()
                .client(bootstrap)
                .url("https://dns.google/dns-query".toHttpUrl())
                .bootstrapDnsHosts(
                    InetAddress.getByAddress(byteArrayOf(8, 8, 8, 8)),
                    InetAddress.getByAddress(byteArrayOf(8, 8, 4, 4)),
                )
                .build(),
            DnsOverHttps.Builder()
                .client(bootstrap)
                .url("https://cloudflare-dns.com/dns-query".toHttpUrl())
                .bootstrapDnsHosts(
                    InetAddress.getByAddress(byteArrayOf(1, 1, 1, 1)),
                    InetAddress.getByAddress(byteArrayOf(1, 0, 0, 1)),
                )
                .build(),
        )
    }

    override fun lookup(hostname: String): List<InetAddress> {
        try {
            return Dns.SYSTEM.lookup(hostname)
        } catch (original: UnknownHostException) {
            for (backup in backups) {
                try {
                    return backup.lookup(hostname)
                } catch (ignored: Exception) {
                    // try the next resolver
                }
            }
            throw original
        }
    }
}
