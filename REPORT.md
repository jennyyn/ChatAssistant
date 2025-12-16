# Project Report

## Challenges I Faced
**Challenge 1: Error Handling**

Problem: Error message was popping up in output that revealed private information about API.

Solution: Only displayed generic, user-friendly message in GUI 

Learned: Always separate developer-facing errors from user-facing messages

**Challenge 2: Handling Cancellation in Async API Calls**

Problem: Cancellation logic in `APISerivce.rewriteTextAsync()` suppressed the error callback when the user pressed the cancel button which caused JUnit test to fail since the test expected an InterruptedException. The UI needed to stop showing up an error popup when the user cancels the request.

Solution: Modified `APIService`so it would always call the error callback and updated the controller's error handler to detect `InterruptedException` and ignore it, prevent error popups when cancellation is intentional

Learned: Separating interal error signaling from UI behavior is important

## Design Pattern Justifications
**Strategy Pattern:** Using the strategy pattern allowed for switching rewriting behavior at runtime without modifying the controller logic. This makes it easy to extend with new modes without touching the core logic

**Factory Pattern:** This helped encapsulate object creation and keep the controller/service code simpler

**Observer Pattern:** Using an Observer-like approach made sure the GUI stays responsive and separate from API logic

## OOP Four Pillars 

### Encapsulation
**Where:** `APIService`, `APIClient`, `RewriteResult`  

**Why:** These classes hide internal details such as HTTP request handling, API keys, or response parsing. External code interacts with them only through public methods (`rewriteText`, `rewriteTextAsync`), reducing complexity and increasing maintainability.

### Abstraction
**Where:** `WritingStrategy` interface  

**Why:** Provides abstraction for text rewriting behavior. The service doesn’t need to know the details of each strategy, it instead just calls `buildPrompt()` and gets a result. This separates what a strategy does from how it does it.

### Inheritance
**Where:** Multiple concrete `WritingStrategy` implementations (`AcademicStrategy`, `ProfessionalStrategy`, `CreativeStrategy`) inheriting from `WritingStrategy`  

**Why:** Allows polymorphic behavior and code reuse. The service can treat all strategies the same while each strategy provides its specific implementation.

### Polymorphism
**Where:** In the class `APIService`,`rewriteText` uses a `WritingStrategy` parameter  

**Why:** At runtime, any concrete strategy can be passed in, and the service doesn’t need to know which one. This allows the same method call to behave differently depending on the strategy instance.


## AI Usage 
1. Used ChatGPT to debug JSON parsing error
   
   Asked: "Is my error handling robust?"
  
   Modified: Used their suggestion to modify parts of my error handling
  
   Verified: Tested bad input and bad wifi/api key
  
3. Used ChatGPT to fix JUnit testing errors
   
   Asked: "Why is my Junit test failing?"
   
   Modified: Used their suggestion to revaluate certain parts of my unit testing
   
   Verified: Reran tests 


