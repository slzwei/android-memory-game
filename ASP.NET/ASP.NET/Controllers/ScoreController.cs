using ASP.NET.DTO;
using Microsoft.AspNetCore.Mvc;

namespace ASP.NET.Controllers;

[ApiController]
[Route("api/[controller]")]
public class ScoreController : ControllerBase
{
    private readonly ScoreService _service;
    private readonly ILogger<ScoreController> _logger;

    public ScoreController(ScoreService service, ILogger<ScoreController> logger)
    {
        _service = service;
        _logger = logger;
    }
    
    //for playFragment add score
    [HttpPost("add")]
    public IActionResult AddScore([FromBody] ScoreRequest scoreRequest)
    {
        try
        {
            _service.AddScore(scoreRequest);
            return Ok();
        }
        catch (Exception exception)
        {
            _logger.LogError("Exception occurred: {Message}", exception.Message);
            return StatusCode(500, "Unexpected error occur.");        }
    }
    //Get the top 5 timings with username from Db
    [HttpGet("topFive")]
    public IActionResult GetTopFive()
    {
        try
        {
            // Logic in ScoreService
            var topFive = _service.TopFive();
            return Ok(topFive);
        }
        catch (Exception exception)
        {
            _logger.LogError("Exception occurred: {Message}", exception.Message);
            return StatusCode(500, "Unexpected error occurred.");
        }
    }
}