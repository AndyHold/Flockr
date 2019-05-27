Feature: The user can manage destinations

  Background:
    Given users with the following information exist:
      | firstName | lastName     | email              | password    |
      | Danny     | Destinations | dd@email.com       | where-to-go |
      | Alice     | Admin        | alice@travelea.com | so-secure   |

  # Test that a 201 code is returned when creating a Destinations with valid data
  Scenario: A user tries to create a destination with complete valid data
    Given that user 0 logged in
    Given the database has been populated with the following countries, districts and destination types:
      | destinationType | country                  | district        |
      | Event           | United States of America | Black Rock City |
      | City            | Australia                | New Farm        |
    Given that I want to create a Destination with the following valid data:
      | destinationName | destinationTypeId | districtId | latitude | longitude | countryId |
      | Lower Hutt      | 1                 | 1          | -41.2    | 174.9     | 1         |
    When I click the Add Destination button
    Then The destination should be created successfully

    # Test that a 400 status code is returned when a user creates a Destination with incomplete data
  Scenario: A user tries to create a destination with no country
    Given that user 0 logged in
    Given the database has been populated with the following countries, districts and destination types:
      | destinationType | country                  | district        |
      | Event           | United States of America | Black Rock City |
      | City            | Australia                | New Farm        |
    Given that I want to create a Destination with the following incomplete data:
      | destinationName | destinationTypeId | districtId | latitude | longitude    |
      | Lower Hutt      | 1                 | 1          | -41.2    | latitudeTest |
    When I click the Add Destination button
    Then I should receive an error, because the data is incomplete

  # Test deleting a destination
  Scenario: A user tries to delete a destination
    Given that user 0 logged in
    And the database has been populated with the following countries, districts and destination types:
      | destinationType | country                  | district        |
      | Event           | United States of America | Black Rock City |
      | City            | Australia                | New Farm        |
    And that I have the following destinations:
      | destinationName           | destinationTypeId | districtId | latitude | longitude | countryId |
      | The Dairy Down The Street | 1                 | 1          | =41.2    | 174.9     | 1         |
    When I click the Delete Destination button
    Then I should receive an error indicating that the Destination is not found

  Scenario: A user can add a photo to a destination
    Given that user 0 logged in
    And the database has been populated with the following countries, districts and destination types:
      | destinationType | country                  | district        |
      | Event           | United States of America | Black Rock City |
      | City            | Australia                | New Farm        |
    And that I have the following destinations:
      | destinationName           | destinationTypeId | districtId | latitude | longitude | countryId |
      | The Dairy Down The Street | 1                 | 1          |  41.2    | 174.9     | 1         |
    And the user has the following photos in the system:
      | filename      | isPrimary | isPublic |
      | monkey.png    | false     | false    |
      | dog.jpg       | false     | false    |
      | cat.jpeg      | false     | false    |
      | cucumber.jpeg | false     | false    |
      | whale.png     | false     | false    |
    When the user adds "monkey.png" to the destination "The Dairy Down The Street"
    Then then the photo gets added to the destination

  Scenario: A user tries to add a photo to a destination with a photo that doesn't exist
    Given that user 0 logged in
    And the database has been populated with the following countries, districts and destination types:
      | destinationType | country                  | district        |
      | Event           | United States of America | Black Rock City |
      | City            | Australia                | New Farm        |
    And that I have the following destinations:
      | destinationName           | destinationTypeId | districtId | latitude | longitude | countryId |
      | The Dairy Down The Street | 1                 | 1          |  41.2    | 174.9     | 1         |
    And the user has the following photos in the system:
      | filename      | isPrimary | isPublic |
      | monkey.png    | false     | false    |
      | dog.jpg       | false     | false    |
      | cat.jpeg      | false     | false    |
      | cucumber.jpeg | false     | false    |
      | whale.png     | false     | false    |
    When the user adds "imageidonthave.png" to the destination "The Dairy Down The Street"
    Then the photo does not get added to the destination

  Scenario: A user tries to add a photo to a destination with a destination that doesn't exist
    Given that user 0 logged in
    And the database has been populated with the following countries, districts and destination types:
      | destinationType | country                  | district        |
      | Event           | United States of America | Black Rock City |
      | City            | Australia                | New Farm        |
    And that I have the following destinations:
      | destinationName           | destinationTypeId | districtId | latitude | longitude | countryId |
      | The Dairy Down The Street | 1                 | 1          |  41.2    | 174.9     | 1         |
    And the user has the following photos in the system:
      | filename      | isPrimary | isPublic |
      | monkey.png    | false     | false    |
      | dog.jpg       | false     | false    |
      | cat.jpeg      | false     | false    |
      | cucumber.jpeg | false     | false    |
      | whale.png     | false     | false    |
    When the user adds "monkey.png" to the destination "Some destination I don't have"
    Then the photo does not get added to the destination

  Scenario: A regular user tries to get all the photos for a destination
    Given that the user 0 is a regular user
    Given that user 0 logged in
    Given the database has been populated with the following countries, districts and destination types:
      | destinationType | country                  | district        |
      | Event           | United States of America | Black Rock City |
      | City            | Australia                | New Farm        |
    Given that I have the following destinations:
      | destinationName           | destinationTypeId | districtId | latitude | longitude | countryId |
      | The Dairy Down The Street | 1                 | 1          |  41.2    | 174.9     | 1         |
    Given the user has the following photos in the system:
      | filename      | isPrimary | isPublic |
      | monkey.png    | false     | true    |
      | dog.jpg       | false     | false    |
      | cat.jpeg      | false     | true    |
      | cucumber.jpeg | false     | false    |
      | whale.png     | false     | false    |
    Given the photo "monkey.png" is linked to the destination "The Dairy Down The Street"
    Given the photo "whale.png" is linked to the destination "The Dairy Down The Street"
    When the user gets all the photos for the destination
    Then the user can see all the public photos for the destination
    And the user can see only their private photos linked to the destination

  Scenario: An admin user tries to get all the photos for a destination
    Given that the user 1 is an admin
    Given that user 1 logged in
    Given that I have the following destinations:
      | destinationName           | destinationTypeId | districtId | latitude | longitude | countryId |
      | The Dairy Down The Street | 1                 | 1          |  41.2    | 174.9     | 1         |
    Given the destination "The Dairy Down The Street" has public photos linked to it
    Given the destination "The Dairy Down The Street" has private photos linked to it
    When the user gets all the photos for the destination
    Then the user can see all the public photos for the destination
    And the admin can see all the user's private photos linked to the destination

  Scenario: A regular user tries to get all the photos for a destination that does not exist
    Given that the user 0 is a regular user
    Given that user 0 logged in
    When the user gets all the photos for a destination that does not exist
    Then they are told that the destination does not exist