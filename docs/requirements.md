# Requirement Specifications

## Non-Functional Requirements

1. **NFR-1. Performance:** The system should be able to handle a high volume of concurrent requests without significant degradation in response time.
2. **NFR-2. Scalability:** The system should be able to scale horizontally to handle increased load.
3. **NFR-3. Availability:** Out of scope of the assignment.
4. **NFR-4. Security:** Out of scope of the assignment.
5. **NFR-5. Maintainability:** The code should be well-structured and easy to understand, to facilitate future maintenance and updates.
6. **NFR-6. Testability:** The system should be designed in a way that allows for comprehensive automated testing.
7. **NFR-7. Monitoring:** Out of scope of the assignment.
8. **NFR-8. Disaster Recovery:** Out of scope of the assignment.
9. **NFR-9. Traceability:** Out of scope of the assignment.

## Functional Requirements

### General Requirements

1. **GT-1. System Error Handling:** The system should return a 500 status code with a message indicating "Internal Server Error" when an unexpected error occurs
   during the processing of a request.

### Money Transfer Functionality

#### Overview

The system must provide functionality to transfer money from one account to another.

#### Requirements

1. **MT-1. Validate Empty Request Body:** The system should return a 400 status code with a message "Request body is required" when a POST request
   to `/transfer` endpoint is made with an empty body.
2. **MT-2. Validate Invalid JSON Request Body:** The system should return a 400 status code with a message "Request body is not valid JSON" when a POST
   request to `/transfer` endpoint is made with an invalid JSON body.
3. **MT-3. Validate Same Accounts for Transfer:** The system should return a 400 status code with a message "Credit account 'account' and debit account '
   account' can't be the same" when a POST request to `/transfer` endpoint is made with a JSON body that specifies the same account for both credit and debit
   accounts.
4. **MT-4. Transfer Money - Same Customer:** The system should return a 200 status code with a valid UUID in the response body when a POST request
   to `/transfer` endpoint is made with a valid JSON body that specifies a transfer between two different accounts of the same customer.
5. **MT-5. Transfer Money - Two Different Customers:** The system should return a 200 status code with a valid UUID in the response body when a POST
   request to `/transfer` endpoint is made with a valid JSON body that specifies a transfer between two different accounts of two different customers.
6. **MT-6. Validate Insufficient Funds:** The system should return a 400 status code with a message indicating "Insufficient funds" when a POST request
   to `/transfer` endpoint is made with a JSON body that specifies a transfer amount greater than the available balance in the debit account.
7. **MT-7. Validate Non-existent Accounts:** The system should return a 400 status code with a message indicating "Account not found" when a POST request
   to `/transfer` endpoint is made with a JSON body that specifies a non-existent account for either the credit or debit account.
8. **MT-8. Validate Negative Transfer Amount:** The system should return a 400 status code with a message indicating "Invalid transfer amount" when a
   POST request to `/transfer` endpoint is made with a JSON body that specifies a negative transfer amount.
9. **MT-9. Concurrent Transfers:** The system should correctly handle concurrent transfer requests, ensuring that account balances are accurately updated
   without any race conditions or data inconsistencies.
10. **MT-10. Handle Duplicate Request IDs in Series:** If the system receives multiple requests with the same request_id in series, it should process the
    first request and ignore subsequent requests with the same request_id. The system should return a 200 status code with the valid UUID of the processed
    request in the response body for all requests with the same request_id.
11. **MT-11. Handle Duplicate Request IDs in Parallel:** If the system receives multiple requests with the same request_id in parallel, it should process only
    one of the requests and ignore the others. The system should return a 200 status code with the valid UUID of the processed request in the response body for
    all requests with the same request_id.
12. **MT-12. Validate Zero Transfer Amount:** The system should return a 400 status code with a message indicating "Invalid transfer amount" when a POST request
    to /transfer endpoint is made with a JSON body that specifies a zero transfer amount. Transfers of zero amount might be considered invalid in your system.
13. **MT-13. Validate Request Body Fields:** The system should return a 400 status code with a message indicating "Invalid request body" when a POST request to
    /transfer endpoint is made with a JSON body that is missing required fields or contains invalid values for those fields.
14. **MT-15. Validate Sender Ownership**: The system should return a 400 status code with a message indicating "Unauthorized operation" when a POST request to
    /transfer endpoint is made with a JSON body that specifies a debit account that does not belong to the customer making the request. This is to prevent
    customers from making transfers from accounts they do not own.