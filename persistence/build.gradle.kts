plugins {
    id("kotlin")
    id("com.squareup.sqldelight")
}

sqldelight {
    database("PlainDb") {
        packageName = "com.m3sv.plainupnp.core.persistence"
    }
}

