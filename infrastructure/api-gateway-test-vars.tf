# thumbprint of the valid SSL certificate for API gateway tests
variable api_gateway_test_valid_certificate_thumbprint {
  type = "string"
  # keeping this empty by default, so that no thumbprint will match
  default = ""
}

# thumbprint of the test certificate that's expired (used by API gateway tests)
variable api_gateway_test_expired_certificate_thumbprint {
  type = "string"
  default = "E2D01C0EF7BCD087E87918FCD16E2AA8AD4B2D01"
}

# thumbprint of the test certificate that's not yet valid (used by API gateway tests)
variable api_gateway_test_not_yet_valid_certificate_thumbprint {
  type = "string"
  # valid since year 2100
  default = "0B1EA240FA2F0CA027342ADE65B41810F651CF8A"
}
