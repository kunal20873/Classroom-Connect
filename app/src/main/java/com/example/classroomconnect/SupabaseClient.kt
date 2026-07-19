package com.example.classroomconnect

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object SupabaseClient {

    val client = createSupabaseClient(
        supabaseUrl = "https://ttansuvasafbnrftfxor.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InR0YW5zdXZhc2FmYm5yZnRmeG9yIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODQzNjEyNjYsImV4cCI6MjA5OTkzNzI2Nn0.H1N5kpPPXA_istidBclQaOTLpSF0w0wKFJ49JeM9nIQ"
    ) {

        install(Postgrest)
        install(Auth)
        install(Storage)

    }
}