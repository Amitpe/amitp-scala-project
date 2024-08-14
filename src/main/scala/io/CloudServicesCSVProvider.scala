package io

import com.github.tototoshi.csv.CSVReader

class CloudServicesCSVProvider {

  def provideCloudServicesMap(): Map[String, String] = {
    val filePath = "src/test/resources/developer_reading_code_with_poetry_books.csv"

    // Create a CSV reader
    val reader = CSVReader.open(filePath)

    try {
      // Read all rows
      val allRows = reader.allWithHeaders()

      // Convert CSV rows to a Map
      allRows.map { row =>
        val serviceName = row("Service name")
        val serviceDomain = row("Service domain")
        serviceDomain -> serviceName
      }.toMap
    }
  }
}