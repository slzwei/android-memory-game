using ASP.NET.DTO;
using ASP.NET.Models;
using Microsoft.EntityFrameworkCore.ChangeTracking;
using System.Security.Cryptography.X509Certificates;

namespace ASP.NET.Controllers;

public class ScoreService
{
    private readonly MemoryGameContext db;

    public ScoreService(MemoryGameContext db)
    {
        this.db = db;
    }

    public void AddScore(ScoreRequest scoreRequest)
    {
        var user = db.User.FirstOrDefault(u => u.Id == scoreRequest.UserId);
        if (user == null) throw new Exception("User not found");

        var score = new Score
        {
            Id = Guid.NewGuid().ToString(),
            Time = scoreRequest.Time,
            UserId = user.Id
        };
        
        db.Score.Add(score);
        db.SaveChanges();
    }

    // get top 5 of the timing and save at DTO/ScoreResult
    public List<ScoreResult> TopFive()
    {
        var scoreboard = from s in db.Score
                         join u in db.User on s.UserId equals u.Id
                         orderby s.Time ascending
                         select new ScoreResult { Username = u.Username, Time = s.Time };

        return scoreboard.Take(5).ToList();
    }



}