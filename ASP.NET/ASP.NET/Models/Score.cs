using System.ComponentModel.DataAnnotations.Schema;

namespace ASP.NET.Models;

public class Score
{
    public Score()
    {
        Id = Guid.NewGuid().ToString();
    }
    
    public string Id { get; set; }
    public int Time { get; set; }

    public string UserId { get; set; }

    [ForeignKey("UserId")]
    public virtual User User { get; set; }
}