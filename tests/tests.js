
exports.defineAutoTests = function() {
  describe('mockgeolocation object existance check', function() {

    it("me.rahul.plugins.mockgeolocation", function () {
      expect( me.rahul.plugins.mockgeolocation).toBeDefined();
    });

    it("me.rahul.plugins.mockgeolocation.check", function() {
      expect( me.rahul.plugins.mockgeolocation.check ).toBeDefined();
    });
  });

  describe('check call test', function() {

    var value;
    var callbacks;

    beforeEach(function(done) {
      callbacks = {
        win: function(arg){
          value = arg;
          done();
        },
        fail: function(err){
          console.log("callbacks.fail");
          done();
        }
      };

      spyOn(callbacks, 'win').and.callThrough();
      spyOn(callbacks, 'fail').and.callThrough();
      
      me.rahul.plugins.mockgeolocation.check("test", callbacks.win, callbacks.fail);
    });

    it("to have been called", function() {
      expect(callbacks.win).toHaveBeenCalled();
    });

    it("check return value", function() {
      expect(value).toBe("test");
    });

  });
};
